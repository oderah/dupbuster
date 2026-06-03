# frozen_string_literal: true

module SigningHelper
  module_function

  def repo_root
    File.expand_path("../..", __dir__)
  end

  def android_aab_path
    File.join(
      repo_root,
      "android/app/build/outputs/bundle/release/app-release.aab"
    )
  end

  def android_prepare_keystore!
    path = ENV["ANDROID_KEYSTORE_FILE"]
    return if path && File.exist?(path)

    encoded = ENV["ANDROID_KEYSTORE_BASE64"]
    UI.user_error!(
      "Set ANDROID_KEYSTORE_FILE or ANDROID_KEYSTORE_BASE64 for release signing"
    ) if encoded.nil? || encoded.empty?

    decoded_path = File.join(ENV.fetch("RUNNER_TEMP", Dir.tmpdir), "dupbuster-release.keystore")
    File.binwrite(decoded_path, Base64.decode64(encoded))
    ENV["ANDROID_KEYSTORE_FILE"] = decoded_path
  end

  def android_gradle_signing_properties
    {
      "android.injected.signing.store.file" => ENV.fetch("ANDROID_KEYSTORE_FILE"),
      "android.injected.signing.store.password" => ENV.fetch("ANDROID_KEYSTORE_PASSWORD"),
      "android.injected.signing.key.alias" => ENV.fetch("ANDROID_KEY_ALIAS"),
      "android.injected.signing.key.password" => ENV.fetch("ANDROID_KEY_PASSWORD")
    }
  end

  def apply_release_version_properties
    version = ENV.fetch("DUPBUSTER_RELEASE_VERSION", "")
    UI.user_error!("Set DUPBUSTER_RELEASE_VERSION (e.g. v1.0.0)") if version.empty?

    code = ENV["DUPBUSTER_VERSION_CODE"]
    props = { "DUPBUSTER_RELEASE_VERSION" => version }
    props["DUPBUSTER_VERSION_CODE"] = code if code && !code.empty?
    props
  end
end
