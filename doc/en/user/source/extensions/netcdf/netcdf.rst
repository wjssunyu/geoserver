.. _netcdf:

NetCDF
======

Adding a NetCDF data store
--------------------------
To add a NetCDF data store the user must go to :guilabel:`Stores --> Add New Store --> NetCDF`.

.. figure:: netcdfcreate.png
   :align: center

   *NetCDF in the list of raster data stores*

Configuring a NetCDF data store
-------------------------------

.. figure:: netcdfconfigure.png
   :align: center

   *Configuring a NetCDF data store*

.. list-table::
   :widths: 20 80

   * - **Option**
     - **Description**
   * - ``Workspace``
     - 
   * - ``Data Source Name``
     - 
   * - ``Description``
     - 
   * - ``Enabled``
     -  
   * - ``URL``
     - 

Notes on supported NetCDFs
--------------------------

The NetCDF plugin for GeoServer supports gridded NetCDF files having dimensions following the COARDS convention (custom, Time, Elevation, Lat, Lon). The NetCDF plugin supports plain NetCDF datasets (.nc files) as well .ncml files (which aggregate and/or modify one or more datasets) and Feature Collections. It supports Forecast Model Run Collection Aggregations (FMRC) either through the NCML or Feature Collection syntax. It supports an unlimited amount of custom dimensions, including runtime. 

`ToolsUI <ftp://ftp.unidata.ucar.edu/pub/netcdf-java/v4.6/toolsUI-4.6.jar>`_ is an useful java tool developed by UCAR which can be useful for a preliminary check on your dataset.
Opening a sample NetCDF using that tool will show an output like this in the Viewer tab:

.. figure:: dataset.png
   :align: center

   *NetCDF viewer in ToolsUI*

* This dataset has 4 dimensions (time, z, lat, lon, marked by the D icon in the left side of the GUI. They have been marked by a blue rectangle in the screenshot).
* Each dimension has an associated independent coordinate variable (marked by the green rectangle).
* Finally, the dataset has 3 geophysical variables, marked by a red rectangle, each having 4 dimensions.

The NetCDF plugin fully supports datasets where each variable's axis is identified by an independent coordinate variable, as shown in the previous example. There is limited support for coordinate variables with two dimensions (see :ref:`netcdf_multidim`), either as part of the definition of a plain dataset (such as y,x/rows,cols/i,j/...) or the result of an aggregation (such as time,runtime - in the case of a runtime aggregation).

A similar dataset will look like this in ToolsUI. Look at the red marked latitude and longitude coordinate variables, each one identified by a y,x 2D matrix.

.. figure:: dataset2dcoords.png
   :align: center

   *NetCDF viewer in ToolsUI for 2D coordinate variables*

.. _netcdf_multidim:

Two-Dimensional Coordinate Variables
--------------------------------------

Two-dimension coordinate variables are exposed in GeoServer as single dimensions. Their domain is exposed in GetCapabilities as a flat list of possible values. However, they imply an interdependence between the different dimensions, where some combinations of values exist (have data) and other combinations do not. For example:

+-------------------+--------------------------------+
|     Runtime       |           Time                 |
+========+==========+==========+==========+==========+
| 	 |          |     0    |     1    |    2     |   
+--------+----------+----------+----------+----------+
| 0      | 1/1/2017 | 1/1/2017 | 1/2/2017 | 1/4/2017 |
+--------+----------+----------+----------+----------+
| 1      | 1/2/2017 | 1/2/2017 | 1/3/2017 |   XXXX   |
+--------+----------+----------+----------+----------+
| 2      | 1/3/2017 | 1/3/2017 |   XXXX   |   XXXX   |
+--------+----------+----------+----------+----------+

The time dimension would thus be exposed in GeoServer as {1/1/2017, 1/2/2017, 1/3/2017, 1/4/2017}. However, the combinations (runtime=1/1/2017, time=1/3/2017), (runtime=1/2/2017, time=1/1/2017), (runtime=1/2/2017, time=1/4/2017) , (runtime=1/3/2017, time=1/1/2017), (runtime=1/3/2017, time=1/2/2017) and (runtime=1/3/2017, time=1/4/2017) do not exist.

Some additional functionality was introduced to maximally exploit two-dimensional coordinate variables:

 * With requests that do not specify certain dimension values, we want to select default values that makes sense with regards to the dimensions values that *were* specified in the request. More specifically we want the maximum or minimum of the domain that matches the specified request's other dimension values; rather than the maximum or minimum of the entire domain.

 * The user may want to query which combination of dimension values do exist and which don't. This can be done through an Auxiliary Vector Store that publishes the entire index. 

A number of system properties allow us to configure this behavior:

 * ``org.geotools.coverage.io.netcdf.param.max``
     A comma separated list of dimensions that must be maximised when their value is absent in the request. In the layer configuration, the default value of these dimensions must be set to 'Built-in'.
 * ``org.geotools.coverage.io.netcdf.param.min``
     A comma separated list of dimensions that must be minimised when their value is absent in the request. In the layer configuration, the default value of these dimensions must be set to 'Built-in'.
 * ``org.geotools.coverage.io.netcdf.auxiliary.store``
     Set to TRUE to display the 'NetCDF Auxiliary Store' option in Geoserver. A NetCDF Auxiliary Store must be published *after* publishing the actual NetCDF store.

The NetCDF Auxiliary Store returns a WFS record like this for each possible combination of dimension values that do not include the two prime spatial dimensions::

  <topp:my-aux-store gml:id="1">
   <topp:the_geom>
    <gml:Polygon srsName="http://www.opengis.net/gml/srs/epsg.xml#4326" srsDimension="2">
     <gml:exterior><gml:LinearRing>
     <gml:posList>259.96003054 -0.04 259.96003054 70.04 310.03999998 70.04 310.03999998 -0.04 259.96003054   -0.04</gml:posList>
     </gml:LinearRing></gml:exterior>
    </gml:Polygon>
   </topp:the_geom>
   <topp:imageindex>160</topp:imageindex>
   <topp:depth>0.0</topp:depth>
   <topp:time>2017-01-01T00:00:00Z</topp:time>
   <topp:runtime>2017-01-02T00:00:00Z</topp:runtime>
  </topp:my-aux-store>

Supporting Custom NetCDF Coordinate Reference Systems
-----------------------------------------------------
Starting with GeoServer 2.8.x, NetCDF related modules (both NetCDF/GRIB store, imageMosaic store based on NetCDF/GRIB dataset and NetCDF output format) allow to support custom Coordinate Reference Systems and Projections.
As reported in the `NetCDF CF documentation, Grid mappings section <http://cfconventions.org/Data/cf-conventions/cf-conventions-1.6/build/cf-conventions.html#appendix-grid-mappings>`_
a NetCDF CF file may expose gridMapping attributes to describe the underlying projection. 

The GeoTools NetCDF machinery will parse the attributes (if any) contained in the underlying NetCDF dataset to setup an OGC CoordinateReferenceSystem object.
Once created, a CRS lookup will be made to identify a custom EPSG (if any) defined by the user to match that Projection.
In case the NetCDF gridMapping is basically the same of the one exposed as EPSG entry but the matching doesn't happen, you may consider tuning the comparison tolerance: See :ref:`crs_configure`, *Increase Comparison Tolerance section*.

User defined NetCDF Coordinate Reference Systems with their custom EPSG need to be provided in :file:`user_projections\\netcdf.projections.properties` file inside your data directory (you have to create that file if missing).  

A sample entry in that property file could look like this:

      971801=PROJCS["lambert_conformal_conic_1SP", GEOGCS["unknown", DATUM["unknown", SPHEROID["unknown", 6371229.0, 0.0]], PRIMEM["Greenwich", 0.0], UNIT["degree", 0.017453292519943295], AXIS["Geodetic longitude", EAST], AXIS["Geodetic latitude", NORTH]], PROJECTION["Lambert_Conformal_Conic_1SP"], PARAMETER["central_meridian", -95.0], PARAMETER["latitude_of_origin", 25.0], PARAMETER["scale_factor", 1.0], PARAMETER["false_easting", 0.0], PARAMETER["false_northing", 0.0], UNIT["m", 1.0], AXIS["Easting", EAST], AXIS["Northing", NORTH], AUTHORITY["EPSG","971801"]]

.. note:: Note the "unknown" names for GEOGCS, DATUM and SPHEROID elements. This is how the underlying NetCDF machinery will name custom elements.
.. note:: Note the number that precedes the WKT. This will determine the EPSG code.  So in this example, the EPSG code is 971801.
.. note:: When dealing with records indexing based on PostGIS, make sure the custom code isn't greater than 998999. (It tooks us a while to understand why we had some issues with custom codes using PostGIS as granules index. Some more details, `here <http://gis.stackexchange.com/questions/145017/why-is-there-an-upper-limit-to-the-srid-value-in-the-spatial-ref-sys-table-in-po>`_)
.. note:: If a parameter like "central_meridian" or "longitude_of_origin" or other longitude related value is outside the range [-180,180], make sure you adjust this value to belong to the standard range. As an instance a Central Meridian of 265 should be set as -95.
 
You may specify further custom NetCDF EPSG references by adding more lines to that file. 

#. Insert the code WKT for the projection at the end of the file (on a single line or with backslash characters)::
     
      971802=PROJCS["lambert_conformal_conic_2SP", \
	    GEOGCS["unknown", \
		  DATUM["unknown", \
		    SPHEROID["unknown", 6377397.0, 299.15550239234693]], \
	      PRIMEM["Greenwich", 0.0], \
		  UNIT["degree", 0.017453292519943295], \
		  AXIS["Geodetic longitude", EAST], \
		  AXIS["Geodetic latitude", NORTH]], \
		PROJECTION["Lambert_Conformal_Conic_2SP"], \
		PARAMETER["central_meridian", 13.333333015441895], \
		PARAMETER["latitude_of_origin", 46.0], \
		PARAMETER["standard_parallel_1", 46.0], \
		PARAMETER["standard_parallel_2", 49], \
		PARAMETER["false_easting", 0.0], \
		PARAMETER["false_northing", 0.0], 
		UNIT["m", 1.0], \
		AXIS["Easting", EAST], \
		AXIS["Northing", NORTH], \
		AUTHORITY["EPSG","971802"]]

#. Save the file.

#. Restart GeoServer.

#. Verify that the CRS has been properly parsed by navigating to the :ref:`srs_list` page in the :ref:`web_admin`.

#. If the projection wasn't listed, examine the logs for any errors.

Specify an external file through system properties
--------------------------------------------------
You may also specify the NetCDF projections definition file by setting a **Java system property** which links to the specified file.
As an instance: :file:`-Dnetcdf.projections.file=/full/path/of/the/customfile.properties`

NetCDF files in read-only directories
-------------------------------------

GeoServer creates hidden index files when accessing NetCDF files. Because these index files are created in the same directory as each NetCDF file, GeoServer will fail to publish NetCDF files if it lacks write access the containing directory.

To permit access to NetCDF files in read-only directories, specify an alternate writeable directory for NetCDF index files by setting the ``NETCDF_DATA_DIR`` Java system property::

    -DNETCDF_DATA_DIR=/path/to/writeable/index/file/directory
