
package uk.gov.gchq.gaffer.graph.connector;

import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPluginType;
import java.util.ArrayList;
import java.util.List;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author GCHQDeveloper601
 */
@ServiceProvider(service = DataAccessPluginType.class)
public class GafferPluginType extends DataAccessPluginType{
 
     public static final String GAFFER = "Gaffer Plugins";

    @Override
    public List<PositionalDataAccessPluginType> getPluginTypeList() {
        final ArrayList<PositionalDataAccessPluginType> pluginTypeList = new ArrayList<>();
        pluginTypeList.add(new DataAccessPluginType.PositionalDataAccessPluginType(GAFFER, 6000));
        return pluginTypeList;
    }
}
