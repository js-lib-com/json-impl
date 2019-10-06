package js.json.impl.unit;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import js.json.impl.Parser;
import junit.framework.TestCase;

public class RegressionsTest extends TestCase
{
  public void testParsingParametersForAppraisalDialogObjectives() throws Throwable
  {
    String json = "[{\"id\":366,\"weighting\":{\"competences\":0.4,\"objectives\":0.6,\"rationale\":\"qq\"},\"objectives\":[{\"definition\":\"mm\",\"measure\":\"gg\",\"concretization\":\"hh\"}]}]";
    Object[] parameters = exercise(json, new Type[]
    {
        Dialog.class
    });
    assertNotNull(parameters);
    Dialog dialog = (Dialog)parameters[0];
    assertNotNull(dialog);
    assertEquals(366, dialog.id);
    assertEquals(0.4, dialog.weighting.competences);
    assertEquals(0.6, dialog.weighting.objectives);
    assertEquals("qq", dialog.weighting.rationale);
    assertEquals(0, dialog.objectives.get(0).id);
    assertEquals(0, dialog.objectives.get(0).rank);
    assertEquals("mm", dialog.objectives.get(0).definition);
    assertEquals("gg", dialog.objectives.get(0).measure);
    assertEquals("hh", dialog.objectives.get(0).concretization);
    assertEquals(0.0, dialog.objectives.get(0).weight);
    assertEquals(0.0, dialog.objectives.get(0).score);
  }

  public void testParsingParametersForSixQsTaskUIUpdate() throws Throwable
  {
    String json = "[{\"id\":87894,\"scenarioId\":6459,\"subscenarioId\":0,\"referenceId\":0,\"userId\":185,\"name\":\"New Event\",\"description\":\"Add description here...\",\"image\":\"http://sixqs.com/img/default/task.png\",\"startDate\":\"1915-07-08T10:23:58.000Z\",\"endDate\":\"1915-07-08T11:23:58.000Z\",\"effort\":0,\"yoffset\":139,\"fixed\":true,\"predecessorsId\":[],\"color\":\"blue\",\"conditionalTask\":false}]";
    Object[] parameters = exercise(json, new Type[]
    {
        TaskUI.class
    });
    assertNotNull(parameters);
    TaskUI task = (TaskUI)parameters[0];
    assertNotNull(task);
    assertEquals(87894, task.id);
    assertEquals(6459, task.scenarioId);
    assertEquals(0, task.subscenarioId);
    assertEquals(0, task.referenceId);
    assertEquals(185, task.userId);
    assertEquals("New Event", task.name);
    assertEquals("Add description here...", task.description);
    assertEquals("http://sixqs.com/img/default/task.png", task.image);

    DateFormat df = new SimpleDateFormat("yyy-MM-dd HH:mm:ss");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    assertEquals("1915-07-08 10:23:58", df.format(task.startDate));
    assertEquals("1915-07-08 11:23:58", df.format(task.endDate));

    assertEquals(0, task.effort);
    assertEquals(139, task.yoffset);
    assertTrue(task.fixed);
    assertEquals(0, task.predecessorsId.size());
    assertEquals("blue", task.color);
    assertFalse(task.conditionalTask);
  }

  public void testParsingObjectSerializedJsonForMusicalInstrumentsEncyclopedia() throws Throwable
  {
    String json = "{\r\n\tname: \"Bansuri\",\r\n\timage: \"bansuri.jpg\",\r\n\tdescription: \"At vero eos et accusamus\",\r\n\tsample: \"bansuri.mp3\",\r\n\tsampleTitle: \"Indian Bansuri Solo\",\r\n\taliases: []\r\n}";
    StringReader reader = new StringReader(json);
    Parser parser = new Parser();
    Instrument instrument = parser.parse(reader, Instrument.class);

    assertNotNull(instrument);
    assertEquals("Bansuri", instrument.name);
    assertEquals("bansuri.jpg", instrument.image);
    assertNull(instrument.bitmap);
    assertEquals("At vero eos et accusamus", instrument.description);
    assertEquals("bansuri.mp3", instrument.sample);
    assertEquals("Indian Bansuri Solo", instrument.sampleTitle);
    assertEquals(0, instrument.aliases.length);
  }

  private static Object[] exercise(String json, Type[] types) throws Throwable
  {
    StringReader reader = new StringReader(json);
    Parser parser = new Parser();
    return parser.parse(reader, types);
  }

  private static class Dialog
  {
    int id;
    Weighting weighting;
    List<Objective> objectives;
  }

  private static class Weighting
  {
    double competences;
    double objectives;
    String rationale;
  }

  private static class Objective
  {
    private int id;
    private int rank;
    private String definition;
    private String measure;
    private String concretization;
    private double weight;
    private double score;
  }

  private static class TaskUI
  {
    int id;
    int scenarioId;
    int subscenarioId;
    int referenceId;
    int userId;
    String name;
    String description;
    String image;
    Date startDate;
    Date endDate;
    long effort;
    int yoffset;
    boolean fixed;
    List<Integer> predecessorsId = new Vector<Integer>(0);
    String color;
    boolean conditionalTask;
  }

  private static class Instrument
  {
    String name;
    String image;
    String bitmap;
    String description;
    String sample;
    String sampleTitle;
    String[] aliases;
  }
}
