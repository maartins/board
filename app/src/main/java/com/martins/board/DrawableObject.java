package com.martins.board;

public interface DrawableObject {
    void setShaderProgramFiles() throws Exception;
    void onSurfaceChanged(float width, float height);
    void draw();
}
