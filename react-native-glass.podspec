require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-glass"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "12.0" }
  s.source       = { :git => "https://github.com/aditya886/react-native-glass.git", :tag => "#{s.version}" }

  # All Objective-C and Objective-C++ source files
  s.source_files = "ios/**/*.{h,m,mm}"

  # Required for both Old Architecture (Paper) and New Architecture (Fabric interop)
  s.dependency "React-Core"
end
