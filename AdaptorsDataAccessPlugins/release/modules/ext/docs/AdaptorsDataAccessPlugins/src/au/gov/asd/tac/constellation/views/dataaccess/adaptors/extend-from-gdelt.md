# Extend From GDELT Knowledge Graph

This plugin extends an existing graph by importing relationships and related entities from the <a href="https://www.gdeltproject.org/">Global Database of Events, Language, and Tone</a>.
<div style="text-align: center">
    <img src="../ext/docs/AdaptorsDataAccessPlugins/src/au/gov/asd/tac/constellation/views/dataaccess/adaptors/resources/GDELTExtension.png" width="80%" alt="GDELT Extension Example"/>
</div>

Only relationships involving existing and selected nodes on the graph will be imported. Any Entities not currently on the graph will be added.
Entities and relationships are imported based on their position in the incoming data. Elements that are received first will be included in the import. 

To understand more about GDELT Entities and Relationships in Constellation, see 
<a href="../ext/docs/AdaptorsDataAccessPlugins/src/au/gov/asd/tac/constellation/views/dataaccess/adaptors/import-entities-from-gdelt.md"> 
    Import Entities From GDELT Help
</a> 
and 
<a href="../ext/docs/AdaptorsDataAccessPlugins/src/au/gov/asd/tac/constellation/views/dataaccess/adaptors/import-relationships-from-gdelt.md"> 
    Import Relationships From GDELT Help
</a>.

## Parameters
-   **Relationship Options** - The type of relationships to import.
-   **Limit** - The maximum number of total relationships to import. The actual number of relationships imported will likely be lower than this limit.

