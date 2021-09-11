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

boolean prettyPrint = true
def imagename = QP.getCurrentServer().getPath().split('/').last().replace('.svs', '').replace('[--series, 0]', '')
def anndir = "/E:/Dropbox (Cambridge University)/Artemisia/Kieren/Kaalund/fourth_run/cortical_ribbons"
def gson = GsonTools.getInstance(prettyPrint)
//def rootdir = QPEx.getProject().getPath().getParent().toString()
def filedir = anndir + '/' + imagename + '_GM_anns.json'
def jf = new File(filedir)

if (jf.exists()) {
    def ann = gson.fromJson(jf.getText(), Map.class)
    def flag = 0
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
        for (vertices in polygons) {
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
    def pathAnnotation = PathObjects.createAnnotationObject(r)
    pathAnnotation.setPathClass(QP.getPathClass("Region"))
    QP.addObject(pathAnnotation)
}
else {
    return
}
