/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.udacity.friendlychat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/*
//READ THIS
-->LINK FOR AUTH DOCUMENTATION
   "https://github.com/firebase/FirebaseUI-Android/tree/master/auth#using-firebaseui-for-authentication"

-->LINK FOR FIREBASE DB DOCUMENTATION
 */

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;

    public static final int RC_SIGN_IN = 1;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private String mUsername;

    // Firebase instance variables
    /*
     *reference to specific part of the DB
     *reference to the message portion of the DB
     *A Firebase reference represents a particular location in your Database and can be used for reading or writing data to that Database location.
     *This class is the starting point for all Database operations.
     *After you've initialized it with a URL, you can use it to read data, write data, and to create new DatabaseReferences.
     */
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;

        // Initialize Firebase components
        /*
         *gets instance of the class
         *acts as the main access point
         */
        mFirebaseDatabase = FirebaseDatabase.getInstance();

        mFirebaseAuth = FirebaseAuth.getInstance();
        /*
         *gives a reference to the root node(--mFirebaseDatabase.getReference()--)
         *using the reference we specify the messages node(--child("messages")--)
         */
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("messages");

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Fire an intent to show an image picker
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                 *onClick upload message to DB then clear the box
                 */
                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(), mUsername, null);
                mMessagesDatabaseReference.push().setValue(friendlyMessage);//saves the data in the dataBase

                // Clear input box
                mMessageEditText.setText("");
            }
        });


        /*
         *Authentication instance creators
         */
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override//initiated AuthState Listener not attached yet
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                //listener attached at onResume detached at onPause
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    onSignedIn(user.getDisplayName());
                } else {
                    onSignedOut();
                    // User is signed out display sign in activity
                    /*
                     *
                     *-->the need of method (createSignInIntentBuilder().)
                     * 1-The set of authentication providers can be specified.
                     * 2-The terms of service URL for your app can be specified,
                     *   which is included as a link in the small-print of the account creation step for new users.
                     *   If no terms of service URL is provided, the associated small-print is omitted.
                     * 3-A custom theme can be specified for the flow,
                     *   which is applied to all the activities in the flow for consistent colors and typography.
                     *-->setAvailableProviders
                     * 1-to add he providers you are willing to allow to sign in to your app
                     *-->setIsSmartLockEnabled(false)
                     * 1-uses Smart Lock for Passwords to store the user's credentials and automatically sign users into your app on subsequent attempts.
                     * 2-Using Smart Lock is recommended to provide the best user experience,
                     *   but in some cases you may want to disable Smart Lock for testing or development.
                     */
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.GoogleBuilder().build(),
                                            new AuthUI.IdpConfig.EmailBuilder().build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
    }



    @Override
    protected void onResume() {//attach the authentication listener
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {//detach the authentication listener
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        /*when layout changes for any reason such as app rotation the onPause is triggered
          that well cause error if any db change occurred in this instance

         */
        detachDBreadListener();
        mMessageAdapter.clear();
    }

    private void onSignedIn(String userName) {
        mUsername = userName;
        attachDBreadListener();
    }

    private void onSignedOut(){
        mUsername = ANONYMOUS;
        mMessageAdapter.clear();
        detachDBreadListener();
    }

    @Override//called when sign in is canceled
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override//sign out option menu item
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
               case R.id.sign_out_menu:
                    AuthUI.getInstance().signOut(this);
                    return true;

               default:
                   return super.onOptionsItemSelected(item);
        }


    }

    private void attachDBreadListener(){
        //receive events about changes in the child locations of a given DatabaseReference ref.
        if(mChildEventListener==null)
        {mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                //This method is triggered when a new child is added to the location to which this listener was added.
                /*
                 * A DataSnapshot instance contains data from a Firebase Database location.
                 * Any time you read Database data, you receive the data as a DataSnapshot.
                 */
                FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                mMessageAdapter.add(friendlyMessage);//triggers the listener
            }

            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}//This method is triggered when the data at a child location has changed.
            public void onChildRemoved(DataSnapshot dataSnapshot) {}//This method is triggered when a child is removed from the location to which this listener was added.
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}//wThis method is triggered when a child location's priority changes.
            //This method will be triggered in the event that this listener either failed at the server, or is removed as a result of the security and Firebase rules.
            public void onCancelled(DatabaseError databaseError) {}
        };
        /*
         *(mMessagesDatabaseReference)-->defines what am listening to.
         *(mChildEventListener)-->defines exactly what happens to the data.
         */
        mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
        }

    }

    private void detachDBreadListener(){
        //the condition for attaching or detaching the listener only once
        if(mChildEventListener != null){
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener=null;
        }
    }
}