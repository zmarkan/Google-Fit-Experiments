package com.zmarkan.fitexperiment;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.fithjoyapp.googlefittest.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.DataPoint;
import com.google.android.gms.fitness.DataSource;
import com.google.android.gms.fitness.DataSourceListener;
import com.google.android.gms.fitness.DataSourcesRequest;
import com.google.android.gms.fitness.DataSourcesResult;
import com.google.android.gms.fitness.DataType;
import com.google.android.gms.fitness.DataTypes;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessScopes;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.SensorRequest;
import com.google.android.gms.fitness.Value;

import java.util.concurrent.TimeUnit;


public class FitActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "FitActivity";
    private static final int REQUEST_OAUTH = 1;
    private GoogleApiClient mClient = null;

    int mInitialNumberOfSteps = 0;
    private TextView mStepsTextView;

    private boolean mFirstCount = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fit);
        mStepsTextView = (TextView) findViewById(R.id.textview_number_of_steps);
    }


    @Override
    protected void onStart() {
        super.onStart();

        mFirstCount = true;
        mInitialNumberOfSteps = 0;

        if (mClient == null || !mClient.isConnected()) {
            connectFitness();
        }
    }

    private void connectFitness() {
        Log.i(TAG, "Connecting...");

        // Create the Google API Client
        mClient = new GoogleApiClient.Builder(this)
                // select the Fitness API
                .addApi(Fitness.API)
                        // specify the scopes of access
                .addScope(FitnessScopes.SCOPE_ACTIVITY_READ)
                .addScope(FitnessScopes.SCOPE_BODY_READ_WRITE)
                .addScope(FitnessScopes.SCOPE_LOCATION_READ)
                        // provide callbacks
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Connect the Google API client
        mClient.connect();
    }

    // Manage OAuth authentication
    @Override
    public void onConnectionFailed(ConnectionResult result) {

        // Error while connecting. Try to resolve using the pending intent returned.
        if (result.getErrorCode() == ConnectionResult.SIGN_IN_REQUIRED ||
                result.getErrorCode() == FitnessStatusCodes.NEEDS_OAUTH_PERMISSIONS) {
            try {
                // Request authentication
                result.startResolutionForResult(this, REQUEST_OAUTH);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Exception connecting to the fitness service", e);
            }
        } else {
            Log.e(TAG, "Unknown connection issue. Code = " + result.getErrorCode());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            if (resultCode == RESULT_OK) {
                // If the user authenticated, try to connect again
                mClient.connect();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // If your connection gets lost at some point,
        // you'll be able to determine the reason and react to it here.
        if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
            Log.i(TAG, "Connection lost.  Cause: Network Lost.");
        } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
            Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        Log.i(TAG, "Connected!");

        // Now you can make calls to the Fitness APIs.
        invokeFitnessAPIs();

    }

    private void invokeFitnessAPIs() {

        // 1. Create a listener object to be called when new data is available
        DataSourceListener listener = new DataSourceListener() {
            @Override
            public void onEvent(DataPoint dataPoint) {

                for (DataType.Field field : dataPoint.getDataType().getFields()) {
                    Value val = dataPoint.getValue(field);
                    updateTextViewWithStepCounter(val.asInt());
                }
            }
        };

        // 1. Specify what data sources to return
        DataSourcesRequest req = new DataSourcesRequest.Builder()
                .setDataSourceTypes(DataSource.TYPE_DERIVED)
                .setDataTypes(DataTypes.STEP_COUNT_DELTA)
                .build();

        // 2. Invoke the Sensors API with:
        // - The Google API client object
        // - The data sources request object
        PendingResult<DataSourcesResult> pendingResult =
                Fitness.SensorsApi.findDataSources(mClient, req);



        // 2. Build a sensor registration request object
        SensorRequest sensorRequest = new SensorRequest.Builder()
                .setDataType(DataTypes.STEP_COUNT_CUMULATIVE)
                .setSamplingRate(1, TimeUnit.SECONDS)
                .build();

        // 3. Invoke the Sensors API with:
        // - The Google API client object
        // - The sensor registration request object
        // - The listener object
        PendingResult<Status> regResult =
                Fitness.SensorsApi.register(mClient, sensorRequest, listener);

        // 4. Check the result asynchronously
        regResult.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.d(TAG, "listener registered");
                    // listener registered
                } else {
                    Log.d(TAG, "listener not registered");
                    // listener not registered
                }
            }
        });
    }

    private void updateTextViewWithStepCounter(final int numberOfSteps) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getBaseContext(), "On Datapoint!", Toast.LENGTH_SHORT);

                if(mFirstCount && (numberOfSteps != 0)) {
                    mInitialNumberOfSteps = numberOfSteps;
                    mFirstCount = false;
                }
                if(mStepsTextView != null){
                    mStepsTextView.setText(String.valueOf(numberOfSteps - mInitialNumberOfSteps));
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mClient.isConnected() || mClient.isConnecting()) mClient.disconnect();
        mInitialNumberOfSteps = 0;
        mFirstCount = true;
    }

}
