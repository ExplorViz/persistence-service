package net.explorviz.persistence.api.v3.model;

import java.util.List;

/**
 * DTO for function calls between two files.
 *
 * @param sourceFileName Name of the source file
 * @param targetFileName Name of the target file
 * @param functions List of function calls between the files
 */
public record FileCommunicationFunctionsDto(
    String sourceFileName, String targetFileName, List<SimpleFunctionDto> functions) {}
