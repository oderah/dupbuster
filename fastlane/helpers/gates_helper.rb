# frozen_string_literal: true

module GatesHelper
  module_function

  def verify_prod_promote_gates!
    script = File.join(SigningHelper.repo_root, "scripts/verify-prod-promote-gates.sh")
    sh("bash", script)
  end
end
