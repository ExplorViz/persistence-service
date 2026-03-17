package net.explorviz.persistence.util;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.explorviz.persistence.proto.CommitData;
import net.explorviz.persistence.proto.FileData;

/** Utility class to map Java exceptions to gRPC exceptions. */
public final class GrpcExceptionMapper {

  private GrpcExceptionMapper() {
    // private constructor to prevent instantiation
  }

  /**
   * Maps an exception to a gRPC RuntimeException.
   *
   * @param e the original exception
   * @param contextInfo optional context string for the error message
   * @return StatusRuntimeException suitable for returning in a gRPC Uni or throwing
   */
  public static StatusRuntimeException mapToGrpcException(
      final Exception e, final String contextInfo) {
    if (e instanceof StatusRuntimeException) {
      return (StatusRuntimeException) e;
    }

    if (e instanceof IllegalArgumentException) {
      return Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException();
    }

    return Status.CANCELLED
        .withDescription("Something went wrong: " + contextInfo + " All changes were rolled back.")
        .asRuntimeException();
  }

  public static StatusRuntimeException mapToGrpcException(
      final Exception e, final FileData fileData) {
    final String contextInfo =
        "Regarding the call to persistFile for the file with hash '"
            + fileData.getFileHash()
            + "'.";
    return mapToGrpcException(e, contextInfo);
  }

  public static StatusRuntimeException mapToGrpcException(
      final Exception e, final CommitData commitData) {
    final String contextInfo =
        "Regarding the call to persistCommit for the commit with hash '"
            + commitData.getCommitId()
            + "'.";
    return mapToGrpcException(e, contextInfo);
  }
}
