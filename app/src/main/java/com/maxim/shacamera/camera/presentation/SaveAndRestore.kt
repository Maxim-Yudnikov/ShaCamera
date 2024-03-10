package com.maxim.shacamera.camera.presentation

interface SaveAndRestore {
    fun save(bundleWrapper: BundleWrapper.Save)
    fun restore(bundleWrapper: BundleWrapper.Restore)
}