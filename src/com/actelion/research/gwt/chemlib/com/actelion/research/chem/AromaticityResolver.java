/*

Copyright (c) 2015-2016, cheminfo

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of {{ project }} nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.actelion.research.chem;

public class AromaticityResolver {
	ExtendedMolecule	mMol;
	private boolean		mAllHydrogensAreExplicit;
	private boolean[]	mIsAromaticBond;
    private int         mAromaticAtoms,mAromaticBonds,mPiElectronsAdded;

    /**
     * Creates a new AromaticityResolver for molecule mol that assumes that
     * all delocalized bonds of mol have bondType Molecule.cBondTypeDelocalized.
     * Internally this constructor creates a bond masks encoding the
     * delocalization from the bond type, then changes these bond type to
     * Molecule.cBondTypeSingle and calls the other constructor.
     * @param mol
     */
    public AromaticityResolver(ExtendedMolecule mol) {
        mMol = mol;

        mol.ensureHelperArrays(Molecule.cHelperNeighbours);

        mIsAromaticBond = new boolean[mol.getBonds()];
        for (int bond=0; bond<mol.getBonds(); bond++) {
            if (mol.getBondType(bond) == Molecule.cBondTypeDelocalized) {
                mIsAromaticBond[bond] = true;
                mol.setBondType(bond, Molecule.cBondTypeSingle);
                }
            }

        initialize();
    	}

    /**
     * Creates a new AromaticityResolver for molecule mol that assumes that
     * all delocalized bonds are flagged properly in isAromaticBond.
     * BondTypes of these bonds are assumed to be Molecule.cBondTypeSingle.
     * @param mol
     * @param isAromaticBond
     */
    public AromaticityResolver(ExtendedMolecule mol, boolean[] isAromaticBond) {
        mMol = mol;
		mIsAromaticBond = isAromaticBond;

        mMol.ensureHelperArrays(Molecule.cHelperNeighbours);

        initialize();
        }

    private void initialize() {
    	mPiElectronsAdded = 0;

    	boolean[] isAromaticAtom = new boolean[mMol.getAtoms()];
        for (int bond=0; bond<mMol.getBonds(); bond++) {
            if (mIsAromaticBond[bond]) {
            	mAromaticBonds++;
            	for (int i=0; i<2; i++) {
            		if (!isAromaticAtom[mMol.getBondAtom(i, bond)]) {
            			isAromaticAtom[mMol.getBondAtom(i, bond)] = true;
        				mAromaticAtoms++;
            			}
            		}
            	}
            }
    	}

    /**
     * This method promotes all necessary bonds of the defined delocalized part of the molecule
     * from single to double bonds in order to create a valid delocalized system
     * of conjugated single and double bonds.
     * Non-cyclic atom chains defined to be delocalized are treated depending
     * on whether we have a molecule or a query fragment. For fragments the respective bond
     * types will be set to cBondTypeDelocalized; for molecules the chain will
     * have alternating single and double bonds starting with double at a non-ring end.
     * @return true if all bonds of the delocalized area could be consistently converted. 
     */
	public boolean locateDelocalizedDoubleBonds() {
		return locateDelocalizedDoubleBonds(false, false);
		}

	/**
	 * This method promotes all necessary bonds of the defined delocalized part of the molecule
	 * from single to double bonds in order to create a valid delocalized system
	 * of conjugated single and double bonds.
	 * Non-cyclic atom chains defined to be delocalized are treated depending
	 * on whether we have a molecule or a query fragment. For fragments the respective bond
	 * types will be set to cBondTypeDelocalized; for molecules the chain will
	 * have alternating single and double bonds starting with double at a non-ring end.
	 * @param mayChangeAtomCharges true if input molecule doesn't carry atom charges and these may be added to achieve aromaticity
	 * @param allHydrogensAreExplicit true this method can rely on all hydrogens being explicitly present
	 * @return true if all bonds of the delocalized area could be consistently converted.
	 */
	public boolean locateDelocalizedDoubleBonds(boolean mayChangeAtomCharges, boolean allHydrogensAreExplicit) {
        if (mAromaticBonds == 0)
            return true;

		mAllHydrogensAreExplicit = allHydrogensAreExplicit;

        RingCollection ringSet = new RingCollection(mMol, RingCollection.MODE_SMALL_RINGS_ONLY);

        if (mMol.isFragment())	
        	promoteDelocalizedChains();

		if (mayChangeAtomCharges)
			addObviousAtomCharges(ringSet);

        // find mandatory conjugation breaking atoms, i.e. atoms whose neighbour bonds must (!) be single bonds
		for (int atom=0; atom<mMol.getAtoms(); atom++) {
			if (isAromaticAtom(atom)) {
				if (ringSet.getAtomRingSize(atom) == 7) {
					// B,C+ in tropylium
					if ((mMol.getAtomicNo(atom) == 5 && mMol.getAtomCharge(atom) == 0)
					 || (mMol.getAtomicNo(atom) == 6 && mMol.getAtomCharge(atom) == 1))
						protectAtom(atom);
					}
				if (ringSet.getAtomRingSize(atom) == 5) {
					// C-,N,O,S in cyclopentadienyl, furan, pyrrol, etc.
					if ((mMol.getAtomicNo(atom) == 6 && mMol.getAtomCharge(atom) == -1)
					 || (mMol.getAtomicNo(atom) == 7 && mMol.getAtomCharge(atom) == 0 && mMol.getAllConnAtoms(atom) == 3)
					 || (mMol.getAtomicNo(atom) == 8 && mMol.getAtomCharge(atom) == 0 && mMol.getConnAtoms(atom) == 2)
					 || (mMol.getAtomicNo(atom) == 16 && mMol.getAtomCharge(atom) == 0 && mMol.getConnAtoms(atom) == 2))
						protectAtom(atom);
					}
				}
			}

		protectAmideBonds(ringSet);
		protectDoubleBondAtoms();

		promoteObviousBonds();

        while (mAromaticBonds != 0) {
            boolean bondsPromoted = false;
            for (int bond=0; bond<mMol.getBonds(); bond++) {
                if (mIsAromaticBond[bond]) {
                    int aromaticConnBonds = 0;
                    for (int j=0; j<2; j++) {
                        int bondAtom = mMol.getBondAtom(j, bond);
                        for (int k=0; k<mMol.getConnAtoms(bondAtom); k++)
                            if (mIsAromaticBond[mMol.getConnBond(bondAtom, k)])
                                aromaticConnBonds++;
                        }
    
                    if (aromaticConnBonds == 4) {
                        promoteBond(bond);
                        promoteObviousBonds();
                        bondsPromoted = true;
                        break;
                        }
                    }
                }

            if (!bondsPromoted) {
                // try to find and promote one entire aromatic 6-ring
                for (int ring=0; ring<ringSet.getSize(); ring++) {
                    if (ringSet.getRingSize(ring) == 6) {
                        boolean isAromaticRing = true;
                        int[] ringBond = ringSet.getRingBonds(ring);
                        for (int i=0; i<6; i++) {
                            if (!mIsAromaticBond[ringBond[i]]) {
                                isAromaticRing = false;
                                break;
                                }
                            }
    
                        if (isAromaticRing) {
                            for (int i=0; i<6; i+=2)
                                promoteBond(ringBond[i]);
                            bondsPromoted = true;
                            break;
                            }
                        }
                    }
                }

            if (!bondsPromoted) {
                // find and promote one aromatic bond
                // (should never happen, but to prevent an endless loop nonetheless)
                for (int bond=0; bond<mMol.getBonds(); bond++) {
                    if (mIsAromaticBond[bond]) {
                        promoteBond(bond);
                        promoteObviousBonds();
                        bondsPromoted = true;
                        break;
                        }
                    }
                }
            }

        return (mAromaticAtoms == mPiElectronsAdded);
		}


	private boolean isAromaticAtom(int atom) {
		for (int i=0; i<mMol.getConnAtoms(atom); i++)
			if (mIsAromaticBond[mMol.getConnBond(atom, i)])
				return true;
		return false;
		}


	private void protectAtom(int atom) {
        mAromaticAtoms--;
		for (int i=0; i<mMol.getConnAtoms(atom); i++) {
			int connBond = mMol.getConnBond(atom, i);
            if (mIsAromaticBond[connBond]) {
                mIsAromaticBond[connBond] = false;
                mAromaticBonds--;
                }
            }
		}


	private void promoteBond(int bond) {
		if (mMol.getBondType(bond) == Molecule.cBondTypeSingle) {
			mMol.setBondType(bond, Molecule.cBondTypeDouble);
			mPiElectronsAdded += 2;
			}

		for (int i=0; i<2; i++) {
			int bondAtom = mMol.getBondAtom(i, bond);
			for (int j=0; j<mMol.getConnAtoms(bondAtom); j++) {
				int connBond = mMol.getConnBond(bondAtom, j);
                if (mIsAromaticBond[connBond]) {
                    mIsAromaticBond[connBond] = false;
                    mAromaticBonds--;
                    }
                }
			}
		}


	private void promoteObviousBonds() {
			// handle bond orders of aromatic bonds along the chains attached to 5- or 7-membered ring
		boolean terminalAromaticBondFound;
		do {
			terminalAromaticBondFound = false;
			for (int bond=0; bond<mMol.getBonds(); bond++) {
				if (mIsAromaticBond[bond]) {
					boolean isTerminalAromaticBond = false;
					for (int i=0; i<2; i++) {
                        int bondAtom = mMol.getBondAtom(i, bond);
					    boolean aromaticNeighbourFound = false;
						for (int j=0; j<mMol.getConnAtoms(bondAtom); j++) {
							if (bond != mMol.getConnBond(bondAtom, j)
							 && mIsAromaticBond[mMol.getConnBond(bondAtom, j)]) {
								aromaticNeighbourFound = true;
								break;
								}
							}
						if (!aromaticNeighbourFound) {
							isTerminalAromaticBond = true;
							break;
							}
						}

					if (isTerminalAromaticBond) {
						terminalAromaticBondFound = true;
						promoteBond(bond);
						}
					}
				}
			} while (terminalAromaticBondFound);
		}


	private void promoteDelocalizedChains() {
        // protect query features cBondQFDelocalized in open aromatic chains of fragments
	    // with incomplete aromatic rings
        for (int bond=0; bond<mMol.getBonds(); bond++) {
            if (mIsAromaticBond[bond]) {
                for (int i=0; i<2; i++) {
                    int terminalAtom = mMol.getBondAtom(i, bond);
                    boolean aromaticNeighbourFound = false;
                    for (int j=0; j<mMol.getConnAtoms(terminalAtom); j++) {
                        if (bond != mMol.getConnBond(terminalAtom, j)
                         && mIsAromaticBond[mMol.getConnBond(terminalAtom, j)]) {
                            aromaticNeighbourFound = true;
                            break;
                            }
                        }
                    if (!aromaticNeighbourFound) {
                        int terminalBond = bond;
                        int bridgeAtom = mMol.getBondAtom(1-i, bond);
                        while (terminalBond != -1) {
                            mIsAromaticBond[terminalBond] = false;
                            mAromaticBonds--;
                            mMol.setBondType(terminalBond, Molecule.cBondTypeDelocalized);
                            terminalBond = -1;
                            terminalAtom = bridgeAtom;
                            for (int j=0; j<mMol.getConnAtoms(terminalAtom); j++) {
                                if (mIsAromaticBond[mMol.getConnBond(terminalAtom, j)]) {
                                    if (terminalBond == -1) {
                                        terminalBond = mMol.getConnBond(terminalAtom, j);
                                        bridgeAtom = mMol.getConnAtom(terminalAtom, j);
                                        }
                                    else {
                                        terminalAtom = -1;
                                        terminalBond = -1;
                                        break;
                                        }
                                    }
                                }
                            }
                        break;
                        }
                    }
                }
            }
        }

	private void protectAmideBonds(RingCollection ringSet) {
		for (int bond=0; bond<mMol.getBonds(); bond++) {
			if (mIsAromaticBond[bond] && ringSet.qualifiesAsAmideTypeBond(bond)) {
				protectAtom(mMol.getBondAtom(0, bond));
				protectAtom(mMol.getBondAtom(1, bond));
				}
			}
		}

	private void protectDoubleBondAtoms() {
		for (int bond=0; bond<mMol.getBonds(); bond++) {
			if (mMol.getBondOrder(bond) == 2) {
				for (int i=0; i<2; i++) {
					int atom = mMol.getBondAtom(i, bond);
					for (int j=0; j<mMol.getConnAtoms(atom); j++) {
						int connBond = mMol.getConnBond(atom, j);
						if (mIsAromaticBond[connBond]) {
							protectAtom(atom);
							break;
							}
						}
					}
				}
			}
		}

	private void addObviousAtomCharges(RingCollection ringSet) {
		// count for every atom of how many rings it is a member
		int[] ringCount = new int[mMol.getAtoms()];
		for (int r=0; r<ringSet.getSize(); r++)
			for (int atom:ringSet.getRingAtoms(r))
				ringCount[atom]++;

		// for all ring atoms add charges and protect preferred delocalization leak atoms
		boolean[] isAromaticRingAtom = new boolean[mMol.getAtoms()];
		for (int ring=0; ring<ringSet.getSize(); ring++) {
			int ringSize = ringSet.getRingSize(ring);
			if (ringSize >= 5 && ringSize <= 7) {
				boolean isDelocalized = true;
				for (int bond:ringSet.getRingBonds(ring)) {
					if (!mIsAromaticBond[bond]) {
						isDelocalized = false;
						break;
						}
					}

				if (isDelocalized) {
					for (int atom:ringSet.getRingAtoms(ring))
						isAromaticRingAtom[atom] = true;

					boolean possible = true;
					int leakAtom = -1;
					int leakPriority = 0;

					for (int atom:ringSet.getRingAtoms(ring)) {
						if (ringSize == 6 || ringCount[atom] > 1) {	// bridgehead atom
							if (!checkAtomTypePi1(atom, false)) {
								possible = false;
								break;
								}
							}
						else {	// non-bridgehead in 5- or 7-membered ring
							int priority = (ringSize == 5) ?
									checkAtomTypeLeak5(atom, false) : checkAtomTypeLeak7(atom, false);
							if (!checkAtomTypePi1(atom, false)) {
								if (leakPriority == 10) {
									possible = false;
									break;
									}
								leakAtom = atom;
								leakPriority = 10;	// MAX
								}
							else if (leakPriority < priority) {
								leakPriority = priority;
								leakAtom = atom;
								}
							}
						}

					if (possible) {
						for (int atom : ringSet.getRingAtoms(ring)) {
							if (atom == leakAtom) {
								if (ringSize == 5)
									checkAtomTypeLeak5(atom, true);
								else
									checkAtomTypeLeak7(atom, true);

								protectAtom(atom);
								}
							else {
								checkAtomTypePi1(atom, true);
								}
							}
						}
					}
				}
			}

		// From here locate delocalized strings of atoms, which are not member
		// of an aromatic ring. Protect preferred atoms and add obvious atom charges.

		// count for every atom the number of delocalized bonds attached
		int[] delocalizedNeighbourCount = new int[mMol.getAtoms()];
		boolean[] hasMetalLigandBond = new boolean[mMol.getAtoms()];
		for (int bond=0; bond<mMol.getBonds(); bond++) {
			int atom1 = mMol.getBondAtom(0, bond);
			int atom2 = mMol.getBondAtom(1, bond);
			if (!isAromaticRingAtom[atom1] && !isAromaticRingAtom[atom2]) {
				if (mIsAromaticBond[bond]) {
					delocalizedNeighbourCount[atom1]++;
					delocalizedNeighbourCount[atom2]++;
					}
				if (mMol.getBondType(bond) == Molecule.cBondTypeMetalLigand) {
					hasMetalLigandBond[atom1] = true;
					hasMetalLigandBond[atom2] = true;
					}
				}
			}

		// From any delocalized atom with one delocalized neighbor (chain end)
		// locate the path to a branch atom (including) or chain end, whatever comes first.
		// Then mark every second atom from the startAtom as not being capable to assume
		// the role of a delocalization leak (priority:-1).
		int[] priority = new int[mMol.getAtoms()];
		int graphAtom[] = new int[mMol.getAtoms()];
		for (int seedAtom=0; seedAtom<mMol.getAtoms(); seedAtom++) {
			if (delocalizedNeighbourCount[seedAtom] == 1) {
				graphAtom[0] = seedAtom;
				int current = 0;
				int highest = 0;
				while (current <= highest) {
					for (int i=0; i<mMol.getConnAtoms(graphAtom[current]); i++) {
						if (mIsAromaticBond[mMol.getConnBond(graphAtom[current], i)]) {
							int candidate = mMol.getConnAtom(graphAtom[current], i);
							if ((current == 0 || candidate != graphAtom[current-1])
							 && delocalizedNeighbourCount[candidate] != 0) {
								graphAtom[++highest] = candidate;
								if ((delocalizedNeighbourCount[candidate] & 1) != 0) {	// 1 or 3
									for (int j=1; j<highest; j+=2)
										priority[graphAtom[j]] = -1;
									highest = 0;	// to break outer loop
									}
								break;
								}
							}
						}
					current++;
					}
				}
			}

		// For every connected delocalized area not being part of an aromatic ring
		// calculate delocalization leak priorities for all atoms not marked above.
		// Then protect the atom with highest priority.
		boolean[] atomHandled = new boolean[mMol.getAtoms()];
		for (int seedAtom=0; seedAtom<mMol.getAtoms(); seedAtom++) {
			if (!atomHandled[seedAtom] && delocalizedNeighbourCount[seedAtom] != 0) {
				graphAtom[0] = seedAtom;
				atomHandled[seedAtom] = true;
				int current = 0;
				int highest = 0;
				while (current <= highest) {
					for (int i = 0; i < mMol.getConnAtoms(graphAtom[current]); i++) {
						if (mIsAromaticBond[mMol.getConnBond(graphAtom[current], i)]) {
							int candidate = mMol.getConnAtom(graphAtom[current], i);
							if (!atomHandled[candidate]) {
								graphAtom[++highest] = candidate;
								atomHandled[candidate] = true;
								}
							}
						}
					current++;
					}

				// if we have an odd number of delocalized atoms in a region, we need to assign a leak
				if ((highest & 1) == 0) {	// highest is atom count-1

					// check for all potential delocalization leak atoms, whether they are compatible
					for (int i = 0; i <= highest; i++)
						if (priority[graphAtom[i]] == 0)
							priority[graphAtom[i]] = checkAtomTypeLeakNonRing(graphAtom[i], false);

					// check for all atoms, which cannot be the leak, whether they can carry a pi-bond
					boolean isPossible = true;
					for (int i = 0; i <= highest; i++) {
						if (priority[graphAtom[i]] <= 0) {
							if (!checkAtomTypePi1(graphAtom[i], false)) {
								isPossible = false;
								break;
								}
							}
						}

					// find the preferred atom for the leak
					if (isPossible) {
						int maxPriority = 0;
						int maxAtom = -1;
						for (int i = 0; i <= highest; i++) {
							if (maxPriority < priority[graphAtom[i]]) {
								maxPriority = priority[graphAtom[i]];
								maxAtom = graphAtom[i];
								}
							}

						if (maxPriority > 0) {
							checkAtomTypeLeakNonRing(maxAtom, true);
							protectAtom(maxAtom);
							}
						}
					}
				}
			}
		}

	/**
	 * Checks, whether the atom is compatible with an aromatic atom of the type
	 * that carries one half of a delocalized double bond.
	 * @param atom
	 * @param correctCharge if true then may add a charge to make the atom compatible
	 * @return
	 */
	private boolean checkAtomTypePi1(int atom, boolean correctCharge) {
		int atomicNo = mMol.getAtomicNo(atom);
		if (atomicNo <5 || atomicNo > 8)
			return false;

		int freeValence = mMol.getFreeValence(atom);
		if (mAllHydrogensAreExplicit && freeValence == 1)
			return true;
		if (!mAllHydrogensAreExplicit && freeValence >= 1)
			return true;

		if (atomicNo != 6) {
			if (freeValence == 0 && mMol.getAtomCharge(atom) == 0) {
				if (correctCharge)
					mMol.setAtomCharge(atom, atomicNo < 6 ? -1 : 1);
				return true;
				}
			}

		return false;
		}

	/**
	 * Checks, whether the atom is compatible with that aromatic atom of
	 * a 5-membered ring that supplies the additional electron pair.
	 * @param atom
	 * @param correctCharge if true then may add a charge to make the atom compatible
	 * @return 0 (not compatible) or priority to be used (higher numbers have higher priority)
	 */
	private int checkAtomTypeLeak5(int atom, boolean correctCharge) {
		if (mMol.getAtomicNo(atom) == 7) {
			if (mMol.getAllConnAtoms(atom) > 2)
				return 3;
			if (mMol.getConnAtoms(atom) == 2)
				return 2;
			}
		if (mMol.getAtomicNo(atom) == 8) {
			return 4;
			}
		if (mMol.getAtomicNo(atom) == 16) {
			if (mMol.getConnAtoms(atom) == 2)
				return 5;
			}
		if (mMol.getAtomicNo(atom) == 6) {
			if (correctCharge)
				mMol.setAtomCharge(atom, -1);
			return 1;
			}
		return 0;
		}


	/**
	 * Checks, whether the atom is compatible with that aromatic atom of
	 * a 7-membered ring that supplies the empty orbital.
	 * @param atom
	 * @param correctCharge if true then may add a charge to make the atom compatible
	 * @return 0 (not compatible) or priority to be used (higher numbers have higher priority)
	 */
	private int checkAtomTypeLeak7(int atom, boolean correctCharge) {
		if (mAllHydrogensAreExplicit) {
			if (mMol.getAllConnAtoms(atom) != 3)
				return 0;
			}
		else {
			if (mMol.getAllConnAtoms(atom) > 3)
				return 0;
			}

		if (mMol.getAtomicNo(atom) == 6) {
			if (correctCharge)
				mMol.setAtomCharge(atom, 1);
			return 2;
			}
		if (mMol.getAtomicNo(atom) == 5) {
			return 4;
			}

		return 0;
		}

	/**
	 * Checks, whether the atom is compatible with the (typically charged) atom
	 * in a delocalized chain of an odd number of atoms that does not carry a pi bond.
	 * @param atom
	 * @param correctCharge if true then may add a charge to make the atom compatible
	 * @return 0 (not compatible) or priority to be used (higher numbers have higher priority)
	 */
	private int checkAtomTypeLeakNonRing(int atom, boolean correctCharge) {
		if (mMol.getAtomCharge(atom) != 0)
			return 0;

		if (mAllHydrogensAreExplicit) {
			if (mMol.getAtomicNo(atom) == 5) {
				if (mMol.getOccupiedValence(atom) != 2)
					return 0;
				if (correctCharge)
					mMol.setAtomCharge(atom, 1);
				return 1;
				}

			if (mMol.getAtomicNo(atom) == 7) {
				if (mMol.getOccupiedValence(atom) != 2)
					return 0;
				if (correctCharge)
					mMol.setAtomCharge(atom, -1);
				return hasMetalNeighbour(atom) ? 6 : 3;
				}

			if (mMol.getAtomicNo(atom) == 8) {
				if (mMol.getOccupiedValence(atom) != 1)
					return 0;
				if (correctCharge)
					mMol.setAtomCharge(atom, -1);
				return hasMetalNeighbour(atom) ? 7 : 4;
				}

			if (mMol.getAtomicNo(atom) == 16) {
				if (mMol.getOccupiedValence(atom) != 1)
					return 0;
				if (correctCharge)
					mMol.setAtomCharge(atom, -1);
				return hasMetalNeighbour(atom) ? 5 : 2;
				}
			}
		else {
			if (mMol.getAtomicNo(atom) == 5) {
				if (mMol.getOccupiedValence(atom) > 2)
					return 0;
				if (correctCharge)
					mMol.setAtomCharge(atom, 1);
				return 1;
				}

			if (mMol.getAtomicNo(atom) == 7) {
				if (mMol.getOccupiedValence(atom) > 2)
					return 0;
				if (correctCharge)
					mMol.setAtomCharge(atom, -1);
				return hasMetalNeighbour(atom) ? 5 : 3;
				}

			if (mMol.getAtomicNo(atom) == 8) {
				if (mMol.getOccupiedValence(atom) > 1)
					return 0;
				if (correctCharge)
					mMol.setAtomCharge(atom, -1);
				return hasMetalNeighbour(atom) ? 7 : 4;
				}

			if (mMol.getAtomicNo(atom) == 16) {
				if (mMol.getOccupiedValence(atom) > 1)
					return 0;
				if (correctCharge)
					mMol.setAtomCharge(atom, -1);
				return hasMetalNeighbour(atom) ? 5 : 2;
				}
			}

		return 0;
		}

	private boolean hasMetalNeighbour(int atom) {
		for (int i=0; i<mMol.getConnAtoms(atom); i++)
			if (mMol.isMetalAtom(mMol.getConnAtom(atom, i)))
				return true;

		return false;
		}
	}
