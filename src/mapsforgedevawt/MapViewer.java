/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Christian Pesch
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014, 2015 devemux86
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mapsforgedevawt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.awt.util.JavaPreferences;
import org.mapsforge.map.awt.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.ReadBuffer;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.poi.awt.storage.AwtPoiPersistenceManagerFactory;
import org.mapsforge.poi.storage.ExactMatchPoiCategoryFilter;
import org.mapsforge.poi.storage.PoiCategoryFilter;
import org.mapsforge.poi.storage.PoiCategoryManager;
import org.mapsforge.poi.storage.PoiPersistenceManager;
import org.mapsforge.poi.storage.PointOfInterest;
import org.mapsforge.poi.storage.UnknownPoiCategoryException;
import sqlInterface.DbManager;
import sqlInterface.RelationTag;

public final class MapViewer {

    private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
    private static final boolean SHOW_DEBUG_LAYERS = false;

    private static final String MESSAGE = "Are you sure you want to exit the application?";
    private static final String TITLE = "Confirm close";

    /**
     * Starts the {@code MapViewer}.
     *
     * @param args command line args: expects the map files as multiple
     * parameters.
     */
    public static void main(String[] args) {
        // Increase read buffer limit
        ReadBuffer.setMaximumBufferSize(6500000);

        String[] arg = new String[1];
        arg[0] = "Kramatorsk1.map";

        List<File> mapFiles = getMapFiles(arg);
        final MapView mapView = createMapView();
        final BoundingBox boundingBox = addLayers(mapView, mapFiles);

        final PreferencesFacade preferencesFacade = new JavaPreferences(Preferences.userNodeForPackage(MapViewer.class));

        final JFrame frame = new JFrame();
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JTextField searchField = new JTextField();
        frame.setTitle("MapViewer");
        panel.add(mapView, BorderLayout.CENTER);
        panel.add(searchField, BorderLayout.NORTH);
        panel.setVisible(true);

        frame.add(panel);
        mapView.addMouseListener(new MapVieweMouseListener(mapView));
        searchField.getDocument().addDocumentListener(new TextFieldChangeListener(searchField));

        frame.pack();
        frame.setSize(new Dimension(800, 600));
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog(frame, MESSAGE, TITLE, JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    mapView.getModel().save(preferencesFacade);
                    mapView.destroyAll();
                    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                }
            }

            @Override
            public void windowOpened(WindowEvent e) {
                final Model model = mapView.getModel();
                // model.init(preferencesFacade);
                byte zoomLevel = LatLongUtils.zoomForBounds(model.mapViewDimension.getDimension(), boundingBox, model.displayModel.getTileSize());
                model.mapViewPosition.setMapPosition(new MapPosition(boundingBox.getCenterPoint(), zoomLevel), false);
            }
        });

        frame.setVisible(true);
    }

    private static BoundingBox addLayers(MapView mapView, List<File> mapFiles) {
        Layers layers = mapView.getLayerManager().getLayers();

        // layers.add(createTileDownloadLayer(tileCache, mapView.getModel().mapViewPosition));
        BoundingBox result = null;
        for (int i = 0; i < mapFiles.size(); i++) {
            File mapFile = mapFiles.get(i);
            TileRendererLayer tileRendererLayer = createTileRendererLayer(createTileCache(i),
                    mapView.getModel().mapViewPosition, true, true, mapFile);
            BoundingBox boundingBox = tileRendererLayer.getMapDataStore().boundingBox();
            result = result == null ? boundingBox : result.extendBoundingBox(boundingBox);
            layers.add(tileRendererLayer);
        }
        if (SHOW_DEBUG_LAYERS) {
            layers.add(new TileGridLayer(GRAPHIC_FACTORY, mapView.getModel().displayModel));
            layers.add(new TileCoordinatesLayer(GRAPHIC_FACTORY, mapView.getModel().displayModel));
        }
        return result;
    }

    private static MapView createMapView() {
        MapView mapView = new MapView();
        mapView.getMapScaleBar().setVisible(true);
        if (SHOW_DEBUG_LAYERS) {
            mapView.getFpsCounter().setVisible(true);
        }

        return mapView;
    }

    private static TileCache createTileCache(int index) {
        TileCache firstLevelTileCache = new InMemoryTileCache(128);
        File cacheDirectory = new File(System.getProperty("java.io.tmpdir"), "mapsforge" + index);
        TileCache secondLevelTileCache = new FileSystemTileCache(1024, cacheDirectory, GRAPHIC_FACTORY);
        return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
    }

    @SuppressWarnings("unused")
    private static Layer createTileDownloadLayer(TileCache tileCache, MapViewPosition mapViewPosition) {
        TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
        TileDownloadLayer tileDownloadLayer = new TileDownloadLayer(tileCache, mapViewPosition, tileSource,
                GRAPHIC_FACTORY);
        tileDownloadLayer.start();
        return tileDownloadLayer;
    }

    private static TileRendererLayer createTileRendererLayer(
            TileCache tileCache,
            MapViewPosition mapViewPosition, boolean isTransparent, boolean renderLabels, File mapFile) {
        TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, new MapFile(mapFile), mapViewPosition, isTransparent,
                renderLabels, false, GRAPHIC_FACTORY);
        tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
        return tileRendererLayer;
    }

    private static List<File> getMapFiles(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("missing argument: <mapFile>");
        }

        List<File> result = new ArrayList<>();
        for (String arg : args) {
            File mapFile = new File(arg);
            if (!mapFile.exists()) {
                throw new IllegalArgumentException("file does not exist: " + mapFile);
            } else if (!mapFile.isFile()) {
                throw new IllegalArgumentException("not a file: " + mapFile);
            } else if (!mapFile.canRead()) {
                throw new IllegalArgumentException("cannot read file: " + mapFile);
            }
            result.add(mapFile);
        }
        return result;
    }

    private MapViewer() {
        throw new IllegalStateException();
    }



    public static class MapVieweMouseListener implements MouseListener {

        MapView mapView;

        public MapVieweMouseListener(MapView mapView) {
            this.mapView = mapView;
        }

        public Collection<PointOfInterest> searchPOI(BoundingBox params) {
            PoiPersistenceManager persistenceManager = null;
            try {
                persistenceManager = AwtPoiPersistenceManagerFactory.getPoiPersistenceManager("Kramatorsk1.poi");
                PoiCategoryManager categoryManager = persistenceManager.getCategoryManager();
                PoiCategoryFilter categoryFilter = new ExactMatchPoiCategoryFilter();
                categoryFilter.addCategory(categoryManager.getPoiCategoryByTitle("Embassies"));
                return persistenceManager.findInRect(params, categoryFilter, null, Integer.MAX_VALUE);
            } catch (UnknownPoiCategoryException ex) {
                Logger.getLogger(MapViewer.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (persistenceManager != null) {
                    persistenceManager.close();
                }
            }
            return null;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            double xPoint = e.getX();
            double yPoint = e.getY();
            int height = mapView.getHeight();
            int width = mapView.getWidth();
            BoundingBox boundingBox = mapView.getBoundingBox();
            double latitudeSpan = boundingBox.getLatitudeSpan();
            double longitudeSpan = boundingBox.getLongitudeSpan();
            double latitude = boundingBox.maxLatitude - (latitudeSpan * yPoint) / height;
            double longitude = (longitudeSpan * xPoint) / width + boundingBox.minLongitude;

            System.out.println(boundingBox + " latitudeSpan =" + latitudeSpan + " longitudeSpan =" + longitudeSpan + "height = " + height + "width = " + width);
            System.out.println("latitude = " + latitude + " longitude = " + longitude);

        }

        @Override
        public void mousePressed(MouseEvent e) {
            //  throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    public static class TextFieldChangeListener implements DocumentListener{
        JTextField searchField;
        DbManager db;
        public TextFieldChangeListener(JTextField searchField){
            this.searchField = searchField;
            db = new DbManager();
            try {
                db.connectToDB("Kram.sqlite");
            } catch (FileNotFoundException ex) {
                Logger.getLogger(MapViewer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
            public void insertUpdate(DocumentEvent e) {
                searchingInfo(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                searchingInfo(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                searchingInfo(e);

            }

        private void searchingInfo(DocumentEvent e) {
            String seachStr = null;
            List<RelationTag> relTags;
            int lengthStr = 0;

            try {
                lengthStr = e.getDocument().getLength();
                seachStr = e.getDocument().getText(0, lengthStr);
            } catch (BadLocationException ex) {
                Logger.getLogger(MapViewer.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println(seachStr + "  " + lengthStr);
            if(lengthStr >=3 ){
                relTags = db.selectRelationTagsByValue(seachStr);
                System.out.println(relTags);
            }
        }

    }
}
