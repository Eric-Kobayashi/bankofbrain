import qupath.lib.scripting.QP
import qupath.lib.gui.scripting.QPEx

def imagename = getCurrentServer().getPath().split('/').last().replace('.svs', '').replace('[--series, 0]', '')
def annotations = getAnnotationObjects()
boolean prettyPrint = true
def gson = GsonTools.getInstance(prettyPrint)
def anns = gson.toJson(annotations)
def rootdir = QPEx.getProject().getPath().getParent().toString()
def filedir = rootdir + '/' + String.format('%s_anns.json', imagename)

File ann = new File(filedir)

ann.newWriter().withWriter {
    ann << anns
}

