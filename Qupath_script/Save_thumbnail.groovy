// Measure distances to WM and GM
import qupath.lib.gui.scripting.QPEx
import qupath.lib.scripting.QP
//import qupath.lib.gui.QuPathGUI
//import qupath.lib.gui.viewer.OverlayOptions
//import qupath.lib.regions.RegionRequest
import qupath.lib.gui.tools.GuiTools;
import qupath.lib.gui.tools.GuiTools.SnapshotType;
import javax.imageio.ImageIO;

// Identify image
imagename = QP.getCurrentServer().getPath().split('/').last().replace('.svs', '')

for (ann in QP.getAnnotationObjects()) {
    if (ann.getPathClass() != null) {
        if (ann.getPathClass().getName() == 'Tumor') {
            ann_WM = ann
        }
        else if (ann.getPathClass().getName() == 'Stroma') {
            ann_GM = ann
        }
        else if (ann.getPathClass().getName() == 'Immune cells') {
            ann_GMGC = ann
        }
    }
    else {
        ann_Tissue = ann
    }
}

try {
    QPEx.setSelectedObject(ann_Tissue)
} catch (Exception ignored) {
    // Slides excluded from analysis
    print(imagename+': No annotation')
    return
}

name = String.format('/E:/Dropbox (Cambridge University)/Artemisia/Kieren/Kaalund/detection/%s_thumbnail.tif', imagename)
q = QPEx.getQuPath()
q.openImageEntry(QPEx.getProjectEntry())
img = GuiTools.makeSnapshot(q, SnapshotType.VIEWER);
fileOutput = new File(name);
ImageIO.write(img, 'TIFF', fileOutput);


//import qupath.lib.gui.tools.GuiTools
//import qupath.lib.roi.RoiTools
//import qupath.lib.scripting.QP
//import qupath.lib.gui.scripting.QPEx
//import qupath.lib.objects.PathObjects
//import qupath.lib.objects.PathAnnotationObject
//import qupath.lib.objects.PathObject
//import qupath.lib.geom.Point2
//import qupath.lib.io.GsonTools
//import qupath.lib.roi.PolygonROI
//import javax.imageio.ImageIO
//
//def imagename = QP.getCurrentServer().getPath().split('/').last().replace('.svs', '').replace('[--series, 0]', '')
//name = String.format('/C:/Users/Eric/Dropbox (Cambridge University)/Artemisia/Kieren/Kaalund/PSP_cohort_Eric/thumbnails/%s_thumbnail.tif', imagename)
//q = QPEx.getQuPath()
//q.openImageEntry(QPEx.getProjectEntry())
//img = GuiTools.makeSnapshot(q, GuiTools.SnapshotType.VIEWER);
//fileOutput = new File(name);
//ImageIO.write(img, 'TIFF', fileOutput);