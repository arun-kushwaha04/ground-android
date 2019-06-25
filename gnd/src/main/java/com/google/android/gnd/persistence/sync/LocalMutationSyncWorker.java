/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gnd.persistence.sync;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.shared.Mutation;

/** A worker that syncs local changes to the remote data store. */
public class LocalMutationSyncWorker extends Worker {

  private static final String TAG = LocalMutationSyncWorker.class.getSimpleName();

  private final LocalDataStore localDataStore;
  private final RemoteDataStore remoteDataStore;

  public LocalMutationSyncWorker(
      @NonNull Context context,
      @NonNull WorkerParameters params,
      LocalDataStore localDataStore,
      RemoteDataStore remoteDataStore) {
    super(context, params);
    this.localDataStore = localDataStore;
    this.remoteDataStore = remoteDataStore;
  }

  @NonNull
  @Override
  public Result doWork() {
    Log.d(TAG, "Connection available; starting sync...");
    localDataStore
        .getPendingMutations()
        .subscribe(
            ms -> {
              for (Mutation m : ms) Log.d(TAG, "Mutation: " + m.getClass() + "   Id: " + m);
            });
    // TODO(#18): Implement me!
    return Result.success();
  }
}
