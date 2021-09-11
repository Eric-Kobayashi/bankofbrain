import qupath.lib.gui.tools.GuiTools
import qupath.lib.roi.RoiTools
import qupath.lib.scripting.QP
import qupath.lib.gui.scripting.QPEx
import qupath.lib.objects.PathObjects
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.objects.PathObject
import qupath.lib.geom.Point2
import qupath.lib.io.GsonTools
import qupath.lib.roi.PolygonROI
import javax.imageio.ImageIO

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
        else if (ann.getPathClass().getName() == 'Others') {
            ann_test = ann
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

boolean prettyPrint = true
def imagename = QP.getCurrentServer().getPath().split('/').last().replace('.svs', '').replace('[--series, 0]', '')
def anndir = "/C:/Users/Eric/Dropbox (Cambridge University)/Artemisia/Kieren/Kaalund/PSP_cohort_Tanrada_test/Tanrada"
def gson = GsonTools.getInstance(prettyPrint)
//def rootdir = QPEx.getProject().getPath().getParent().toString()
def filedir = anndir + '/' + imagename + '_anns.json'
def jf = new File(filedir)
if (jf.exists()) {
    def anns = gson.fromJson(jf.getText(), ArrayList.class)
    def flag = 0
    for (ann in anns) {
        def polygons = ann['geometry']['coordinates']
        if (ann['geometry']['type'] == "MultiPolygon") {
            for (p in polygons) {
                for (vertices in p) {
                    def points = vertices.collect { new Point2(it[0], it[1]) }
                    points.removeLast()
                    def polygon = new PolygonROI(points)
                    if (flag == 0) {
                        r = polygon
                        flag = 1
                    } else {
                        r = RoiTools.combineROIs(r, polygon, RoiTools.CombineOp.ADD)
                    }
                }
            }
        } else if (ann['geometry']['type'] == "Polygon") {
            for (vertices in p) {
                def points = vertices.collect { new Point2(it[0], it[1]) }
                points.removeLast()
                def polygon = new PolygonROI(points)
                if (flag == 0) {
                    r = polygon
                    flag = 1
                } else {
                    r = RoiTools.combineROIs(r, polygon, RoiTools.CombineOp.ADD)
                }
            }
        }
        //    to_be_added << pathAnnotation
        //    ann_neu = PathObjects.createAnnotationObject(ann.getROI()
    }
    r = RoiTools.combineROIs(r, ann_Tissue.getROI(), RoiTools.CombineOp.INTERSECT)
    def pathAnnotation = PathObjects.createAnnotationObject(r)
    pathAnnotation.setPathClass(QP.getPathClass("Region"))
    QP.addObject(pathAnnotation)
//def to_be_added = []
}
else {
    return
}


try {
    QPEx.setSelectedObject(ann_test)
} catch (Exception ignored) {
    // Not included in Jaccard analysis
    return
}

// Calculate Jaccard index
r1 = RoiTools.combineROIs(r, ann_test.getROI(), RoiTools.CombineOp.INTERSECT)
r2 = RoiTools.combineROIs(r, ann_test.getROI(), RoiTools.CombineOp.ADD)
def jaccard = r1.getArea() / r2.getArea()
print(String.format('S%s: %s', imagename, jaccard.toString()))
//name = String.format('/C:/Users/Eric/Dropbox (Cambridge University)/Artemisia/Kieren/Kaalund/PSP_cohort_Eric/thumbnails/%s_thumbnail.tif', imagename)
//q = QPEx.getQuPath()
//q.openImageEntry(QPEx.getProjectEntry())
//img = GuiTools.makeSnapshot(q, GuiTools.SnapshotType.VIEWER);
//fileOutput = new File(name);
//ImageIO.write(img, 'TIFF', fileOutput);
