# Import Entities From GDELT Knowledge Graph

This plugin imports entities from the <a href="https://www.gdeltproject.org/">Global Database of Events, Language, and Tone</a>.
<div style="text-align: center">
    <img src="../ext/docs/AdaptorsDataAccessPlugins/src/au/gov/asd/tac/constellation/views/dataaccess/adaptors/resources/GDELTEntityImport.png" width="80%" alt="GDELT Entity import Example"/>
</div>
Entities are imported based on their position in the incoming data. Elements that are received first will be included in the import. 

A range of different entity types can be imported using this plugin. These entity types are described as follows. 

**Person** - An entity that represents a single person. Typically an individual that may be real or fictional. Contains the attribute:

-   Identifier: *Person's Full Name*

**Organisation** - An entity that represents a collection of people. Typically, a business, government, or charity. Contains the attribute:

-   Identifier: *Organisation's Name*

**Theme** - An entity that represents a theme in a source. There are currently over 300 themes recognised by GDELT. Contains the attributes:

-   Identifier: *Theme Identifier*

**Location** - An entity that represents a place. Typically, a City, State or Country. Contains the attributes:

-   Identifier: *Location's Extended Name*
-   Geo.Country: *Country of the Location*
-   Geo.Longitude: *Longitudinal Coordinates*
-   Geo.Latitude: *Latitudinal Coordinates*

**Source** - A general point of origin that entity information has been extracted from. Typically, an online publication, forum or site. Contains the attribute:

-   Identifier: *Source's Name*

**URL** - A specific point of origin that entity information has been extracted from. Typically, an online publication, forum or site. Contains the attribute:

-   Identifier: *The Entire URL*

## Parameters
-   **Entity Options** - The type of entities to import.
-   **Limit** - The maximum number of total entities to import. The actual number of entities imported will likely be lower than this limit.

