# Constellation Adaptors
[Constellation](https://github.com/constellation-app/constellation) is a 
graph-focused data visualisation and interactive analysis application enabling 
data access, federation and manipulation capabilities across large and complex 
data sets.

While it provides significant capability out of the box, it has been designed 
with extensibility and modularity in mind. The 
[Core](https://github.com/constellation-app/constellation) is intended to be 
domain agnostic and therefore does not connect to external systems. The 
purpose of this module suite is to hold a collection of Constellation 
"Adaptors" that can be used to connect to specific data sources, providers 
for maps, adaptors for systems like elastic search, Data Access Plugins etc.

# List of Adaptors

## Adaptors Functionality

A collection of Data Access View plugins that you can use to import, enrich and 
hop using a GraphML or Pajek file formats.

## Adaptors Map View

A collection of Map View providers including ArcGIS, Bing, GoogleMap, 
OpenStreetMap and Staman which use publically available APIs.

## Adaptors Dependencies

Used to hold 3rd party libraries required by a adaptors. Presently no additional 
dependencies exist.

## Contributing to Constellation Adaptors

For more information please see the [contributing guide](https://github.com/constellation-app/constellation/blob/master/CONTRIBUTING.md).

## More Information
This repository should follow everything mentioned in the Constellation 
[README](https://github.com/constellation-app/constellation/blob/master/README.md).
