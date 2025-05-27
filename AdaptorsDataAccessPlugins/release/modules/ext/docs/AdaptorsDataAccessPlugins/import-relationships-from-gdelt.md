# Import Relationships From GDELT Knowledge Graph

This plugin imports relationships between entities from the <a href="https://www.gdeltproject.org/">Global Database of Events, Language, and Tone</a>.
<div style="text-align: center">
    <img src="../ext/docs/AdaptorsDataAccessPlugins/resources/GDELTRelationshipImport.png" width="80%" alt="GDELT Entity import Example"/>
</div>

A range of different entity relationships can be imported using this plugin. Entity relationships are imported based on their position in the incoming data. Relationships that are received first will be included in the import. 
If a relationship is between 1 or more entities currently on the graph, these entities will be references in the relationship. If a relationship is between 1 or more entities that are not included in the current graph, these entities will be added. 
Due to this fact relationship imports are not dependant on existing entities on the graph. For graph dependent GDELT Relationship imports, see 
<a href="../ext/docs/AdaptorsDataAccessPlugins/extend-from-gdelt.md"> 
    Extend From GDELT Help
</a>

Descriptions on entities can be found in 
<a href="../ext/docs/AdaptorsDataAccessPlugins/import-entities-from-gdelt.md"> 
    Import Entities From GDELT Help
</a>.

## Parameters
-   **Relationship Options** - The type of relationships to import.
-   **Limit** - The maximum number of total relationships to import. The actual number of relationships imported will likely be lower than this limit.
