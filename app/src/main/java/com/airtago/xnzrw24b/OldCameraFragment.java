package com.airtago.xnzrw24b;


import android.hardware.Camera;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;


/**
 * A simple {@link Fragment} subclass.
 */
public class OldCameraFragment extends Fragment implements CameraFragmentInterface {


    public OldCameraFragment() {
        // Required empty public constructor
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static OldCameraFragment newInstance() {
        OldCameraFragment fragment = new OldCameraFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.old_fragment_camera, container, false);
    }

    private Camera camera;
    private OldCameraPreview cameraPreview;
    private AimView aimView;

    public void setLevel(double level) {
        aimView.setLevel(level);
        aimView.invalidate();
    }

    public void clearRadius() {
        aimView.setRadius(0);
        aimView.invalidate();
    }

    private Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e) {
            e.printStackTrace();
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    public void onStart() {
        super.onStart();

        //Adding camera preview
        camera = getCameraInstance();
        if (camera != null) {
            cameraPreview = new OldCameraPreview(getActivity(), camera);
            View view = getActivity().findViewById(R.id.old_camera_preview);
            android.widget.FrameLayout layout = (android.widget.FrameLayout) getActivity().findViewById(R.id.old_camera_preview);
            layout.addView(cameraPreview);
        }

        //Adding aim drawer
        RelativeLayout camLayout = (RelativeLayout)(getActivity().findViewById(R.id.oldCamLayout));
        aimView = new AimView(camLayout.getContext());
        camLayout.addView(aimView);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (camera != null)
            camera.release();
        camera = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (camera == null) {
            camera = getCameraInstance();
            if (camera != null && cameraPreview != null) {
                cameraPreview.camera = camera;
            }
        }
    }
}
