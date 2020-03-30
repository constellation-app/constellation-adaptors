/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.gov.asd.tac.constellations.dataaccess.adaptors;

import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPluginType;
import au.gov.asd.tac.constellations.dataaccess.adaptors.providers.gaffer.GafferRoadExampleQueries;
import java.util.ArrayList;
import java.util.List;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author GCHQDeveloper601
 */
@ServiceProvider(service = DataAccessPluginType.class)
public class DataAccessPluginAdaptorsType extends DataAccessPluginType {

    public static final String GAFFER_ROAD_EXAMPLE = "Gaffer Road Example";

    @Override
    public List<PositionalDataAccessPluginType> getPluginTypeList() {
        final ArrayList<PositionalDataAccessPluginType> pluginTypeList = new ArrayList<>();
        pluginTypeList.add(new DataAccessPluginType.PositionalDataAccessPluginType(GAFFER_ROAD_EXAMPLE, 2500));
      
        return pluginTypeList;
    }

}
