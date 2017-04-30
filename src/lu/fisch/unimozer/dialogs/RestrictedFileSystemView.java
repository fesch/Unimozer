/*
 * http://stackoverflow.com/questions/2354920/jfilechooser-shows-only-d-drive-content
 */

package lu.fisch.unimozer.dialogs;


import java.io.File;
import java.util.ArrayList;

import javax.swing.filechooser.FileSystemView;

public class RestrictedFileSystemView extends FileSystemView
{
    private File[] allowed = null;

    public RestrictedFileSystemView(){

    }

    public RestrictedFileSystemView(File[] allowed){
        this.allowed = allowed;
    }


    // apply filter here
    @Override
    public File[] getRoots(){
        if(allowed != null){
            return allowed;
        }

        File[] files = super.getRoots();
        java.util.List allow = new ArrayList();
        for(int i=0; i<files.length; i++){
            File desktop = files[i];
            File[] roots = desktop.listFiles();
            int rl = roots.length;
            for(int j=0; j<rl; j++){
                File cr = roots[j];
                File[] sroots = cr.listFiles();
                if(sroots != null){
                    for(int k=0; k<sroots.length; k++){
                        File cr1 = sroots[k];
                        String path = cr1.getAbsolutePath();
                        if(path.equals("D:\\")){
                            allow.add(cr1);
                        }
                    }
                }
            }
        }
        allowed = (File[])allow.toArray(new File[0]);
        return allowed;
    }

    @Override
    public File createNewFolder(File dir){
        return null;
    }

    @Override
    public boolean isHiddenFile(File f) {
        return super.isHiddenFile(f);
    }

    @Override
    public boolean isRoot(File f){
        return super.isRoot(f);
    }
}
