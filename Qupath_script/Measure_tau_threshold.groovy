import qupath.lib.scripting.QP
import qupath.lib.gui.scripting.QPEx


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

QPEx.selectCells()
QPEx.addPixelClassifierMeasurements("tau", "tau")
QPEx.saveDetectionMeasurements(String.format('/E:/Dropbox (Cambridge University)/Artemisia/Kieren/Kaalund/tau_stains/%s_cells.txt', imagename), "Name", "Centroid X µm", "Centroid Y µm", "tau: Necrosis area µm^2")

QPEx.selectAnnotations();
QPEx.addPixelClassifierMeasurements("tau", "tau")
QPEx.saveAnnotationMeasurements(String.format('/E:/Dropbox (Cambridge University)/Artemisia/Kieren/Kaalund/tau_stains/%s_all.txt', imagename))

