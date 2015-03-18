# Documentation

[Molecule](#molecule)  
[SDF parser](#sdfileparser)  
[Substructure search](#sssearch)  
[Substructure search with index](#sssearchwithindex)

---------------------------------------

## Molecule

### Molecule.fromSmiles(smiles, [options])

Parse the provided `smiles` and return a `Molecule`.  
By default, stereo features are parsed, which triggers itself a coordinate computation and coordinates are computed again after parsing to guarantee that they are always the same.  
If you do not need stereo features and want the fastest parsing, use this method with `{noCoordinates: true, noStereo: true}`.

__Options__

* `noCoordinates` - disable extra coordinate computation (default: false).
* `noStereo` - disable stereo features parsing (default: false).

### Molecule.fromMolfile(molfile)

Parse the provided `molfile` and return a `Molecule`

### Molecule.fromIDCode(idcode, [ensure2DCoordinates|coordinates])

Parse the provided `idcode` and return a `Molecule`

__Arguments__

* `idcode` - string with the idcode
* `ensure2DCoordinates` - boolean indicating if the 2D coordinates should be computed (default: true)
* `coordinates` - string with the idcoordinates to use

### molecule.toSmiles()

Returns a smiles string

### molecule.toMolfile()

Returns a molfile (V2000) string

### molecule.getIDCode()

Returns the ID Code if the molecule

### molecule.getIDCoordinates()

Returns a string representation of the coordinates of the atoms in the molecule

### molecule.getIDCodeAndCoordinates()

Returns an object with both the idcode and coordinates

### molecule.getMolecularFormula()

Returns a [MolecularFormula](#molecularformula) object

### molecule.getProperties()

Returns a [MoleculeProperties](#moleculeproperties) object

### molecule.getIndex()

Returns the int[] index array used for substructure search

### molecule.inventCoordinates()

Compute and set atom coordinates for this molecule

### molecule.setFragment(isFragment)

Flags the molecule as a fragment or unflags it (useful for substructure search)

### molecule.getFragmentNumbers()

Returns the number of fragments in the molecule

### molecule.getFragments()

Returns an array of fragments from the molecule

### molecule.expandHydrogens()

Add all implicit hydrogens to the molecule

### molecule.getDiastereotopicAtomIDs([element])

TODO

## MolecularFormula

### mf.getAbsoluteWeight()
### mf.getRelativeWeight()
### mf.getFormula()

## MoleculeProperties

### p.getAcceptorCount()
### p.getDonorCount()
### p.getLogP()
### p.getLogS()
### p.getPolarSurfaceArea()
### p.getRotatableBondCount()
### p.getStereoCenterCount()

---------------------------------------

## SDFileParser

### new SDFileParser(sdf, [fields])

Create a new parser

__Arguments__

* `sdf` - string with the sdf
* `fields` - array of field names to parse. If null, the sdf is scanned to find all possible names (not efficient)

### parser.next()

Move to the next molfile. Returns true if there is one, false otherwise.

__Example__
```js
var sdf = fs.readFileSync('./mysdf.sdf');
var parser = new actelion.SDFileParser(sdf);
while(parser.next()) {
    var molecule = parser.getMolecule();
    // process molecule
}
```

### parser.getMolecule()

Returns the current [`Molecule`](#molecule)

### parser.getMolfile()

Returns the current molfile string

### parser.getField(name)

Returns the content of the field `name` from the current record or null

### parser.getFieldNames()

Returns the list of field names for the entire sdf

---------------------------------------

## SSSearch

Basic substructure search

### new SSSearch()

Create a new substructure searcher

### search.setFragment(fragment)

Set the `fragment` to search

__Arguments__

* `fragment` - [Molecule](#molecule) instance to set as fragment. It has to be flagged with [`setFragment(true)`](#moleculesetfragmentisfragment) first

### search.setMolecule(molecule)

Set the target `molecule` in which the search will be done

__Arguments__

* `molecule` - [Molecule](#molecule) instance to set as target molecule

### search.isFragmentInMolecule()

Returns true if the set fragment is in the target molecule, false otherwise

---------------------------------------

## SSSearchWithIndex

Fast substructure search with index filtering

### SSSearchWithIndex.getKeyIDCode()

Returns an array of the 512 idcodes that are used for the index

### SSSearchWithIndex.getTanimotoSimilarity(index1, index2)

Returns the Tanimoto similarity between the two indexes

### new SSSearchWithIndex()

Create a new substructure searcher

### search.setFragment(fragment, [index])

Set the `fragment` to search

__Arguments__

* `fragment` - [Molecule](#molecule) instance to set as fragment. It has to be flagged with [`setFragment(true)`](#moleculesetfragmentisfragment) first
* `index` - If the index for this fragment was computed previously, it can be provided here to save time

### search.setMolecule(molecule, [index])

Set the target `molecule` in which the search will be done

__Arguments__

* `molecule` - [Molecule](#molecule) instance to set as target molecule
* `index` - If the index for this fragment was computed previously, it can be provided here to save time

### search.isFragmentInMolecule()

Returns true if the set fragment is in the target molecule, false otherwise