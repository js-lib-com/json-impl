package js.json.impl.unit;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import js.json.impl.ErrorReporter;
import js.util.Classes;
import junit.framework.TestCase;

public class SupportClassesUnitTest extends TestCase
{
  public void testGenericTypesSupport() throws SecurityException, NoSuchMethodException
  {
    Method method = this.getClass().getDeclaredMethod("persons", new Class[]
    {
        List.class
    });

    Type[] parameterTypes = method.getGenericParameterTypes();
    assertNotNull(parameterTypes);
    assertEquals(1, parameterTypes.length);
    ParameterizedType type = (ParameterizedType)parameterTypes[0];
    assertEquals("interface java.util.List", type.getRawType().toString());
    assertEquals("class java.net.URL", type.getActualTypeArguments()[0].toString());

    Type returnType = method.getGenericReturnType();
    type = (ParameterizedType)returnType;
    assertEquals("interface java.util.List", type.getRawType().toString());
    assertEquals("class java.io.File", type.getActualTypeArguments()[0].toString());
  }

  @SuppressWarnings("unused")
  private List<File> persons(List<URL> persons)
  {
    return Collections.emptyList();
  }

  public void testErrorReporterCircularBuffer() throws Throwable
  {
    String sample = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.";
    assertEquals("d do eiusmod tempor incididunt ut labore et dolore magna aliqua.", exerciseBuffer(sample));

    final int BUFFER_SIZE = Classes.getFieldValue(ErrorReporter.class, "BUFFER_SIZE");
    assertEquals("Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed d", exerciseBuffer(sample.substring(0, BUFFER_SIZE - 1)));
    assertEquals("Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do", exerciseBuffer(sample.substring(0, BUFFER_SIZE)));
    assertEquals("orem ipsum dolor sit amet, consectetur adipisicing elit, sed do ", exerciseBuffer(sample.substring(0, BUFFER_SIZE + 1)));
  }

  private static String exerciseBuffer(String sample) throws Throwable
  {
    ErrorReporter errorReporter = new ErrorReporter();
    for(int i = 0; i < sample.length(); i++) {
      errorReporter.store(sample.charAt(i));
    }
    return errorReporter.streamSample();
  }
}
