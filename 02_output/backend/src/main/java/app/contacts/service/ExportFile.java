package app.contacts.service;

/**
 * An Export file (R-016-01).
 *
 * @param fileName the file name of R-016-06
 * @param content the {@code .xlsx} content
 */
public record ExportFile(String fileName, byte[] content) {

  /** Keeps its own copy of the content. */
  public ExportFile {
    content = content.clone();
  }

  /**
   * A copy of the content.
   *
   * @return the {@code .xlsx} content
   */
  @Override
  public byte[] content() {
    return content.clone();
  }
}
