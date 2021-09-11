import qupath.lib.gui.scripting.QPEx
import qupath.lib.scripting.QP

def imagename = QPEx.getCurrentServer().getPath().split('/').last().replace('.svs[--series, 0]', '')

for (ann in QP.getAnnotationObjects()) {
    if (ann.getPathClass() != null) {
        if (ann.getPathClass().getName() == 'Tumor') {
            ann_WM = ann
        } else if (ann.getPathClass().getName() == 'Stroma') {
            ann_GM = ann
        } else if (ann.getPathClass().getName() == 'Immune cells') {
            ann_GMGC = ann
        }
    } else {
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
selectAnnotations()
saveAnnotationMeasurements(String.format('E:/Dropbox (Cambridge University)/Artemisia/Kieren/Kaalund/fifth_run/all_areas/%s_area.txt', imagename))
