package com.bumptech.glide;


import static com.bumptech.glide.test.Matchers.anyDrawable;
import static com.bumptech.glide.test.Matchers.anyDrawableTarget;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemoryCacheAdapter;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.test.ConcurrencyHelper;
import com.bumptech.glide.test.GlideApp;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.test.TearDownGlide;
import com.bumptech.glide.util.Util;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class LoadDrawableTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();

  @Mock private RequestListener<Drawable> listener;

  private Context context;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    context = InstrumentationRegistry.getTargetContext();
  }

  @Test
  public void clear_withLoadedBitmapDrawable_doesNotRecycleBitmap() {
    Glide.init(context, new GlideBuilder()
        .setMemoryCache(new MemoryCacheAdapter())
        .setBitmapPool(new BitmapPoolAdapter()));
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
    FutureTarget<Drawable> target =
        concurrency.wait(
            GlideApp.with(context)
                .load(drawable)
                .submit(100, 100));
    Glide.with(context).clear(target);

    // Allow Glide's resource recycler to run on the main thread.
    concurrency.pokeMainThread();

    assertThat(bitmap.isRecycled()).isFalse();
  }

  @Test
  public void transform_withLoadedBitmapDrawable_doesNotRecycleBitmap() {
    Glide.init(context, new GlideBuilder()
        .setMemoryCache(new MemoryCacheAdapter())
        .setBitmapPool(new BitmapPoolAdapter()));
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
    concurrency.wait(
        GlideApp.with(context)
            .load(drawable)
            .centerCrop()
            .submit(100, 100));

    assertThat(bitmap.isRecycled()).isFalse();
  }

  @Test
  public void loadFromRequestManager_withBitmap_doesNotLoadFromDiskCache() {
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
    Glide.init(context, new GlideBuilder()
        .setMemoryCache(new LruResourceCache(Util.getBitmapByteSize(bitmap) * 10))
        .setBitmapPool(new LruBitmapPool(Util.getBitmapByteSize(bitmap) * 10)));
    FutureTarget<Drawable> target =
        concurrency.wait(
            GlideApp.with(context)
                .load(drawable)
                .centerCrop()
                .submit(100, 100));
    Glide.with(context).clear(target);

    assertThat(bitmap.isRecycled()).isFalse();

    concurrency.runOnMainThread(new Runnable() {
      @Override
      public void run() {
        Glide.get(context).clearMemory();
      }
    });

    concurrency.wait(
        GlideApp.with(context)
            .load(drawable)
            .centerCrop()
            .listener(listener)
            .submit(100, 100));

    verify(listener)
        .onResourceReady(
            anyDrawable(),
            any(),
            anyDrawableTarget(),
            eq(DataSource.LOCAL),
            anyBoolean());
  }

  @Test
  public void loadFromRequestBuilder_asDrawable_withBitmap_doesNotLoadFromDiskCache() {
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
    Glide.init(context, new GlideBuilder()
        .setMemoryCache(new LruResourceCache(Util.getBitmapByteSize(bitmap) * 10))
        .setBitmapPool(new LruBitmapPool(Util.getBitmapByteSize(bitmap) * 10)));
    FutureTarget<Drawable> target =
        concurrency.wait(
            GlideApp.with(context)
                .asDrawable()
                .load(drawable)
                .centerCrop()
                .submit(100, 100));
    Glide.with(context).clear(target);

    assertThat(bitmap.isRecycled()).isFalse();

    concurrency.runOnMainThread(new Runnable() {
      @Override
      public void run() {
        Glide.get(context).clearMemory();
      }
    });

    concurrency.wait(
        GlideApp.with(context)
            .load(drawable)
            .centerCrop()
            .listener(listener)
            .submit(100, 100));

    verify(listener)
        .onResourceReady(
            anyDrawable(),
            any(),
            anyDrawableTarget(),
            eq(DataSource.LOCAL),
            anyBoolean());
  }

  @Test
  public void loadFromRequestBuilder_asDrawable_withBitmapAndStrategyBeforeLoad_notFromCache() {
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
    Glide.init(context, new GlideBuilder()
        .setMemoryCache(new LruResourceCache(Util.getBitmapByteSize(bitmap) * 10))
        .setBitmapPool(new LruBitmapPool(Util.getBitmapByteSize(bitmap) * 10)));
    FutureTarget<Drawable> target =
        concurrency.wait(
            GlideApp.with(context)
                .asDrawable()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .load(drawable)
                .centerCrop()
                .submit(100, 100));
    Glide.with(context).clear(target);

    assertThat(bitmap.isRecycled()).isFalse();

    concurrency.runOnMainThread(new Runnable() {
      @Override
      public void run() {
        Glide.get(context).clearMemory();
      }
    });

    concurrency.wait(
        GlideApp.with(context)
            .load(drawable)
            .centerCrop()
            .listener(listener)
            .submit(100, 100));

    verify(listener)
        .onResourceReady(
            anyDrawable(),
            any(),
            anyDrawableTarget(),
            eq(DataSource.LOCAL),
            anyBoolean());
  }
}
