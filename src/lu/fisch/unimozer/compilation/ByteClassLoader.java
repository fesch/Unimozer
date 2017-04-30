/**
 * @author Andrew Davison 2007
 * @link   http://fivedots.coe.psu.ac.th/~ad/jg/javaArt1/onTheFlyArt1.pdf
 */

package lu.fisch.unimozer.compilation;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.JavaFileObject;

public class ByteClassLoader extends ClassLoader
{
    // global
    private Map<String, JavaFileObject> store;

    private String rootDirectory = null;

    public ByteClassLoader(Map<String, JavaFileObject> str, String rootDirectory)
    {
      super( ByteClassLoader.class.getClassLoader() );   // set parent
      this.rootDirectory=rootDirectory;
      store = str;
    }


    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException
    {
      JavaFileObject jfo = store.get(name);    // load java file object
      if (jfo == null)
        throw new ClassNotFoundException(name);

      byte[] bytes = ((ByteArrayJFO)jfo).getByteArray();
                                     // get byte codes array
      Class cl = defineClass(name, bytes, 0, bytes.length);
                                     // send byte codes to the JVM
      if (cl == null)
        throw new ClassNotFoundException(name);
      return cl;
    } // end of findClass()

    @Override
    public URL getResource(String name)
    {
        URL res = super.getResource(name);
        //System.out.println("Looking for       : "+name);
        //System.out.println("Parent loader sais: "+res);
        //System.out.println("RootDirectory is  : "+rootDirectory);
        //System.out.println("Resource name is  : "+rootDirectory+System.getProperty("file.separator")+name);
        if(res==null && rootDirectory!=null)
        {
            File f = new File(rootDirectory+System.getProperty("file.separator")+name);
            if(f.exists()) try
            {
                res = f.toURI().toURL();
            }
            catch (MalformedURLException ex)
            {
                System.err.println(ex.getMessage());
            }
        }
        return res;
    }

}
