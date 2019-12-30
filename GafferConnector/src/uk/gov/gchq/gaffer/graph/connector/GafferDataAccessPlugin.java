/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.gov.gchq.gaffer.graph.connector;

import au.gov.asd.tac.constellation.graph.processing.RecordStore;
import au.gov.asd.tac.constellation.pluginframework.Plugin;
import au.gov.asd.tac.constellation.pluginframework.PluginException;
import au.gov.asd.tac.constellation.pluginframework.PluginInteraction;
import au.gov.asd.tac.constellation.pluginframework.parameters.PluginParameters;
import au.gov.asd.tac.constellation.views.dataaccess.DataAccessPlugin;
import au.gov.asd.tac.constellation.views.dataaccess.templates.RecordStoreQueryPlugin;
import java.util.logging.Logger;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 *
 * @author GCHQDeveloper601
 */
@ServiceProviders({
    @ServiceProvider(service = DataAccessPlugin.class)
    ,
    @ServiceProvider(service = Plugin.class)
})
@Messages("GafferDataAccessPlugin=Basic Gaffer Plugin")
public class GafferDataAccessPlugin extends RecordStoreQueryPlugin implements DataAccessPlugin {

    private static final Logger LOGGER = Logger.getLogger(GafferDataAccessPlugin.class.getName());

    @Override
    public String getType() {
        return GafferPluginType.GAFFER;
    }

    @Override
    public int getPosition() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected RecordStore query(RecordStore query, PluginInteraction interaction, PluginParameters parameters) throws InterruptedException, PluginException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
