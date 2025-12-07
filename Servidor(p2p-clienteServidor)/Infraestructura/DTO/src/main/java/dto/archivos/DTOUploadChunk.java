package dto.archivos;

import com.google.gson.annotations.SerializedName;

public class DTOUploadChunk {
    private String uploadId;
    private int chunkNumber;

    @SerializedName(value = "chunkDataBase64", alternate = {"chunkData_base64"})
    private String chunkDataBase64;

    public DTOUploadChunk() {}

    public DTOUploadChunk(String uploadId, int chunkNumber, String chunkDataBase64) {
        this.uploadId = uploadId;
        this.chunkNumber = chunkNumber;
        this.chunkDataBase64 = chunkDataBase64;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public int getChunkNumber() {
        return chunkNumber;
    }

    public void setChunkNumber(int chunkNumber) {
        this.chunkNumber = chunkNumber;
    }

    public String getChunkDataBase64() {
        return chunkDataBase64;
    }

    public void setChunkDataBase64(String chunkDataBase64) {
        this.chunkDataBase64 = chunkDataBase64;
    }
}
