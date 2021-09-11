import qupath.lib.scripting.QP
import qupath.lib.gui.scripting.QPEx

def imagename = QP.getCurrentServer().getPath().split('/').last().replace('.svs', '').replace('[--series, 0]', '')
def annotations = QP.getAnnotationObjects()
boolean prettyPrint = true
def flag = 0
def gson = GsonTools.getInstance(prettyPrint)
for (annot in annotations) {
    try {
        def classname = annot.getPathClass().getName()
        if (classname == 'Region') {
            anns = gson.toJson(annot)
            flag = 1
            break
        }
    }
    catch (Exception ignored) {continue}
}

if (flag == 1) {
    def rootdir = "/E:/Dropbox (Cambridge University)/Artemisia/Kieren/Kaalund/sixth_run/cortical_ribbons"
    def filedir = rootdir + '/' + String.format('%s_ribbon_anns.json', imagename)
    File ann = new File(filedir)
    ann.newWriter().withWriter {
        ann << anns
    }
}
