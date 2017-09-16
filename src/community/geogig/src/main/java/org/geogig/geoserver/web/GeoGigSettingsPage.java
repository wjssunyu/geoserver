/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web;

import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.geogig.geoserver.web.settings.CacheStatusPanel;
import org.geoserver.web.GeoServerSecuredPage;

public class GeoGigSettingsPage extends GeoServerSecuredPage {

    /** serialVersionUID */
    private static final long serialVersionUID = -6228795354577370186L;

    protected AjaxTabbedPanel<ITab> tabbedPanel;

    protected FeedbackPanel feedbackPanel;

    public GeoGigSettingsPage() {
        this.feedbackPanel = super.feedbackPanel;
        initUI();
    }

    protected void initUI() {
        add(new CacheStatusPanel("cacheStatus"));
    }
}
