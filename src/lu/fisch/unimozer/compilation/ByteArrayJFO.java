/**
 * @author Andrew Davison 2007
 * @link   http://fivedots.coe.psu.ac.th/~ad/jg/javaArt1/onTheFlyArt1.pdf
 */

package lu.fisch.unimozer.compilation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import javax.tools.SimpleJavaFileObject;

public class ByteArrayJFO extends SimpleJavaFileObject
{
    private ByteArrayOutputStream baos = null;

    public ByteArrayJFO(String className, Kind kind) throws Exception
    {
        super( new URI(className), kind);
    }

    @Override
    public InputStream openInputStream() throws IOException
    // the input stream to the java file object accepts bytes
    {
        return new ByteArrayInputStream(baos.toByteArray());
    }

    @Override
    public OutputStream openOutputStream() throws IOException
    // the output stream supplies bytes
    {
        return baos = new ByteArrayOutputStream();
    }

    public byte[] getByteArray()
    // access the byte output stream as an array
    {
        return baos.toByteArray();
    }

}
