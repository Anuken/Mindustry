package mindustry.android;

import android.content.*;
import arc.util.*;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.games.*;
import mindustry.service.*;

public class GPGameService extends GameService{
    private GoogleSignInAccount account;

    public void onResume(Context context){
        Log.info("[GooglePlayService] Resuming.");

        GoogleSignInAccount current = GoogleSignIn.getLastSignedInAccount(context);

        GoogleSignInOptions options =
        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
        .requestScopes(Games.SCOPE_GAMES_SNAPSHOTS)
        .build();

        if(GoogleSignIn.hasPermissions(current, options.getScopeArray())){
            this.account = current;
            Log.info("Already signed in to Google Play Games.");
        }else{
            GoogleSignIn.getClient(context, options).silentSignIn().addOnCompleteListener(complete -> {
                if(!complete.isSuccessful()){
                    if(complete.getException() != null){
                        Log.err("Failed to sign in to Google Play Games.", complete.getException());
                    }else{
                        Log.warn("Failed to sign in to Google Play Games.");
                    }
                }else{
                    this.account = complete.getResult();
                    Log.info("Signed in to Google Play Games.");
                }
            });
        }
    }
}
