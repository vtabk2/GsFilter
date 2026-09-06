package com.gsfilter.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.PictureDrawable;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.caverock.androidsvg.SVG;
import com.gsfilter.filter.glide.FilterThumbnailModel;
import com.gsfilter.filter.glide.FilterThumbnailModelLoader;
import com.gsfilter.glide.svg.SvgDecoder;
import com.gsfilter.glide.svg.SvgDrawableTranscoder;

import java.io.InputStream;

@GlideModule
public final class GsFilterGlideModule extends AppGlideModule {
    @Override
    public void registerComponents(
            @NonNull Context context,
            @NonNull Glide glide,
            @NonNull Registry registry
    ) {
        registry.register(SVG.class, PictureDrawable.class, new SvgDrawableTranscoder())
                .append(InputStream.class, SVG.class, new SvgDecoder())
                .append(FilterThumbnailModel.class, Bitmap.class, new FilterThumbnailModelLoader.Factory());
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
