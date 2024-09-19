package com.example.a30;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameActivity extends AppCompatActivity { // Изменено на public
    private CustomView customView;
    private Button loadModelButton, closeModelButton, resetButton;
    private TextView polyCountLabel, modelSizeLabel;

    private static final int PICK_MODEL_REQUEST = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        customView = new CustomView(this);
        setContentView(R.layout.activity_main);
        initializeUI();
        setListeners();
    }

    private void initializeUI() {
        loadModelButton = findViewById(R.id.loadModelButton);
        closeModelButton = findViewById(R.id.closeModelButton);
        resetButton = findViewById(R.id.resetButton);
        polyCountLabel = findViewById(R.id.polyCountLabel);
        modelSizeLabel = findViewById(R.id.modelSizeLabel);
        closeModelButton.setEnabled(false);
    }

    private void setListeners() {
        loadModelButton.setOnClickListener(v -> openFileChooser());
        closeModelButton.setOnClickListener(v -> customView.closeModel());
        resetButton.setOnClickListener(v -> customView.resetModel());
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        startActivityForResult(intent, PICK_MODEL_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_MODEL_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                loadModel(uri);
            }
        }
    }

    private void loadModel(Uri uri) {
        Log.d("GameActivity", "Loading model from URI: " + uri.toString());

        File file = new File(uri.getPath());

        if (!file.exists()) {
            Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            customView.loadOBJ(br);
            closeModelButton.setEnabled(customView.hasModel());
        } catch (IOException e) {
            Toast.makeText(this, "Error loading OBJ file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("GameActivity", "Error loading OBJ file", e);
        }
    }

    private class CustomView extends View {
        private final List<double[]> vertices = new ArrayList<>();
        private final List<int[]> faces = new ArrayList<>();

        private boolean isRotating = true;
        private double angleX = 0, angleY = 0;
        private double scale = 100;
        private double translateX = 0, translateY = 0;
        private int lastMouseX, lastMouseY;
        private boolean dragging = false;

        private final Paint paint = new Paint();

        public CustomView(GameActivity context) {
            super(context);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            setFocusable(true);
            setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        lastMouseX = (int) event.getX();
                        lastMouseY = (int) event.getY();
                        dragging = true;
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE && dragging) {
                        translateModel(event);
                        invalidate();
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        dragging = false;
                    }
                    return true;
                }
            });
            startRotationTimer();
        }

        private void startRotationTimer() {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isRotating) {
                        angleX += 0.01;
                        angleY += 0.01;
                        invalidate();
                    }
                    postDelayed(this, 16);
                }
            }, 16);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            drawShape(canvas);
        }

        private void drawShape(Canvas canvas) {
            if (!vertices.isEmpty()) {
                for (double[] vertex : vertices) {
                    float x = (float) (vertex[0] * scale + getWidth() / 2);
                    float y = (float) (-vertex[1] * scale + getHeight() / 2);
                    canvas.drawCircle(x, y, 5, paint);
                }
            }
        }

        public void resetModel() {
            translateX = 0;
            translateY = 0;
            angleX = 0;
            angleY = 0;
            invalidate();
        }

        public void closeModel() {
            vertices.clear();
            faces.clear();
            initializeCube();
            invalidate();
        }

        private void loadOBJ(BufferedReader br) throws IOException {
            vertices.clear();
            faces.clear();
            String line;
            while ((line = br.readLine()) != null) {
                processOBJLine(line);
            }
            invalidate();
        }

        private void processOBJLine(String line) {
            if (line.startsWith("v ")) {
                String[] parts = line.trim().split("\\s+");
                vertices.add(new double[]{
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3])
                });
            } else if (line.startsWith("f ")) {
                String[] parts = line.trim().split("\\s+");
                int[] face = Arrays.stream(parts).
                        skip(1).
                        mapToInt(part -> Integer.parseInt(part.split("/")[0]) - 1).
                        toArray();
                faces.add(face);
            }
        }

        public boolean hasModel() {
            return !vertices.isEmpty() && !faces.isEmpty();
        }

        private void translateModel(MotionEvent event) {
            double mouseX = (event.getX() - getWidth() / 2) / scale;
            double mouseY = (event.getY() - getHeight() / 2) / scale;
            translateX += mouseX - ((lastMouseX - getWidth() / 2) / scale);
            translateY += (mouseY - ((lastMouseY - getHeight() / 2) / scale)) * 100.0;
        }

        private void initializeCube() {
            vertices.add(new double[]{-1, -1, -1});
            vertices.add(new double[]{1, -1, -1});
            vertices.add(new double[]{1, 1, -1});
            vertices.add(new double[]{-1, 1, -1});
            vertices.add(new double[]{-1, -1, 1});
            vertices.add(new double[]{1, -1, 1});
            vertices.add(new double[]{1, 1, 1});
            vertices.add(new double[]{-1, 1, 1});

            faces.add(new int[]{0, 1, 2, 3});
            faces.add(new int[]{4, 5, 6, 7});
            faces.add(new int[]{0, 1, 5, 4});
            faces.add(new int[]{2, 3, 7, 6});
            faces.add(new int[]{0, 3, 7, 4});
            faces.add(new int[]{1, 2, 6, 5});
            invalidate();
        }
    }
}
