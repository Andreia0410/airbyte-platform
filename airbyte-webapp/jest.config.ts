import type { Config } from "jest";

const isInCI = process.env.CI;

const jestConfig: Config = {
  verbose: true,
  // Required to overwrite the default which would ignore node_modules from transformation,
  // but several node_modules are not transpiled so they would fail without babel transformation running
  transformIgnorePatterns: [],
  maxWorkers: isInCI ? 4 : "100%",
  snapshotSerializers: ["./src/test-utils/classname-serializer.js"],
  coveragePathIgnorePatterns: ["\\.stories\\.tsx$"],
  modulePathIgnorePatterns: ["src/.*/__mocks__"],
  testEnvironment: "jsdom",
  moduleDirectories: ["node_modules", "src"],
  moduleNameMapper: {
    "\\.module\\.scss$": "test-utils/mock-data/mockIdentity.js",
    "\\.(css|png|scss)$": "test-utils/mock-data/mockEmpty.js",
    "\\.svg$": "test-utils/mock-data/mockSvg.js",
  },
  setupFilesAfterEnv: ["./src/test-utils/setup-tests.ts"],
  globalSetup: "./src/test-utils/global-setup.js",
};

export default jestConfig;
