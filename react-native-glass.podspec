require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-glass"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]

  # author must be a Hash in CocoaPods — string format causes parse failure
  s.author       = { "Aditya" => "adiaditya144@gmail.com" }

  s.platforms    = { :ios => "12.0" }

  # Use a local path during development; tag-based for releases
  s.source       = {
    :git => "https://github.com/aditya886/react-native-glass.git",
    :tag => "#{s.version}"
  }

  # All Objective-C and Objective-C++ source files in ios/
  s.source_files = "ios/**/*.{h,m,mm}"

  # Works on Old Architecture (Paper) and New Architecture (Fabric interop)
  s.dependency "React-Core"
end
