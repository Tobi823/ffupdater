package de.marmaro.krt.ffupdater.utils

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test

class UtilsTest {

    @Test
    fun getVersionAndCodenameForApiLevel_withAndroidR_returnCorrectText() {
        assertEquals("11 (R)", Utils.getVersionAndCodenameForApiLevel(Build.VERSION_CODES.R))
    }

    @Test
    fun name() {
        val data = """    
2021-02-11T13:41:33     INFO - Running scriptworker version 36.0.1
2021-02-11T13:41:33     INFO - build_task_dependencies scriptworker EW47NZ4ETr6mCJm9eVWmyQ
2021-02-11T13:41:33     INFO - find_sorted_task_dependencies scriptworker EW47NZ4ETr6mCJm9eVWmyQ
2021-02-11T13:41:33     INFO - found dependencies: [('scriptworker:parent', 'BpEU_djMQ8-3PWWPuaxPkQ'), ('scriptworker:build', 'E7UJGIXgQlKtDOd1YNciZA')]
2021-02-11T13:41:33    DEBUG -  scriptworker:parent BpEU_djMQ8-3PWWPuaxPkQ is docker-worker
2021-02-11T13:41:33    DEBUG -  makedirs(/app/workdir/cot/BpEU_djMQ8-3PWWPuaxPkQ)
2021-02-11T13:41:33     INFO - build_task_dependencies scriptworker:parent BpEU_djMQ8-3PWWPuaxPkQ
2021-02-11T13:41:33     INFO - find_sorted_task_dependencies scriptworker:parent BpEU_djMQ8-3PWWPuaxPkQ
2021-02-11T13:41:33     INFO - found dependencies: [('scriptworker:parent:parent', 'An8owHqsSvC9f-9Yok5Z8Q')]
2021-02-11T13:41:33    DEBUG -  scriptworker:parent:parent An8owHqsSvC9f-9Yok5Z8Q is docker-worker
2021-02-11T13:41:33    DEBUG -  makedirs(/app/workdir/cot/An8owHqsSvC9f-9Yok5Z8Q)
2021-02-11T13:41:33     INFO - build_task_dependencies scriptworker:parent:parent An8owHqsSvC9f-9Yok5Z8Q
2021-02-11T13:41:33     INFO - find_sorted_task_dependencies scriptworker:parent:parent An8owHqsSvC9f-9Yok5Z8Q
2021-02-11T13:41:33     INFO - found dependencies: [('scriptworker:parent:parent:parent', 'An8owHqsSvC9f-9Yok5Z8Q')]
2021-02-11T13:41:33    DEBUG -  scriptworker:build E7UJGIXgQlKtDOd1YNciZA is docker-worker
2021-02-11T13:41:33    DEBUG -  makedirs(/app/workdir/cot/E7UJGIXgQlKtDOd1YNciZA)
2021-02-11T13:41:33     INFO - build_task_dependencies scriptworker:build E7UJGIXgQlKtDOd1YNciZA
2021-02-11T13:41:33     INFO - find_sorted_task_dependencies scriptworker:build E7UJGIXgQlKtDOd1YNciZA
2021-02-11T13:41:33     INFO - found dependencies: [('scriptworker:build:parent', 'BpEU_djMQ8-3PWWPuaxPkQ'), ('scriptworker:build:docker-image', 'XGGmdDBTSPOa1ZFu6Roldg')]
2021-02-11T13:41:33    DEBUG -  scriptworker:build:docker-image XGGmdDBTSPOa1ZFu6Roldg is docker-worker
2021-02-11T13:41:33    DEBUG -  makedirs(/app/workdir/cot/XGGmdDBTSPOa1ZFu6Roldg)
2021-02-11T13:41:33     INFO - build_task_dependencies scriptworker:build:docker-image XGGmdDBTSPOa1ZFu6Roldg
2021-02-11T13:41:33     INFO - find_sorted_task_dependencies scriptworker:build:docker-image XGGmdDBTSPOa1ZFu6Roldg
2021-02-11T13:41:33     INFO - found dependencies: [('scriptworker:build:docker-image:parent', 'Lt9aXLUtSU2lhskGjQCJLg')]
2021-02-11T13:41:33    DEBUG -  scriptworker:build:docker-image:parent Lt9aXLUtSU2lhskGjQCJLg is docker-worker
2021-02-11T13:41:33    DEBUG -  makedirs(/app/workdir/cot/Lt9aXLUtSU2lhskGjQCJLg)
2021-02-11T13:41:33     INFO - build_task_dependencies scriptworker:build:docker-image:parent Lt9aXLUtSU2lhskGjQCJLg
2021-02-11T13:41:33     INFO - find_sorted_task_dependencies scriptworker:build:docker-image:parent Lt9aXLUtSU2lhskGjQCJLg
2021-02-11T13:41:33     INFO - found dependencies: [('scriptworker:build:docker-image:parent:parent', 'Lt9aXLUtSU2lhskGjQCJLg')]
2021-02-11T13:41:33     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/BpEU_djMQ8-3PWWPuaxPkQ/artifacts/public%2Fchain-of-trust.json
2021-02-11T13:41:33     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/BpEU_djMQ8-3PWWPuaxPkQ/artifacts/public%2Fchain-of-trust.json.sig
2021-02-11T13:41:33     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/An8owHqsSvC9f-9Yok5Z8Q/artifacts/public%2Fchain-of-trust.json
2021-02-11T13:41:33     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/An8owHqsSvC9f-9Yok5Z8Q/artifacts/public%2Fchain-of-trust.json.sig
2021-02-11T13:41:33     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/E7UJGIXgQlKtDOd1YNciZA/artifacts/public%2Fchain-of-trust.json
2021-02-11T13:41:34    DEBUG -  makedirs(/app/workdir/cot/An8owHqsSvC9f-9Yok5Z8Q/./public)
2021-02-11T13:41:34     INFO - Done
2021-02-11T13:41:34     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/E7UJGIXgQlKtDOd1YNciZA/artifacts/public%2Fchain-of-trust.json.sig
2021-02-11T13:41:34    DEBUG -  makedirs(/app/workdir/cot/E7UJGIXgQlKtDOd1YNciZA/./public)
2021-02-11T13:41:34     INFO - Done
2021-02-11T13:41:34     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/XGGmdDBTSPOa1ZFu6Roldg/artifacts/public%2Fchain-of-trust.json
2021-02-11T13:41:34     INFO - Done
2021-02-11T13:41:34     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/XGGmdDBTSPOa1ZFu6Roldg/artifacts/public%2Fchain-of-trust.json.sig
2021-02-11T13:41:34    DEBUG -  makedirs(/app/workdir/cot/BpEU_djMQ8-3PWWPuaxPkQ/./public)
2021-02-11T13:41:34     INFO - Done
2021-02-11T13:41:34     INFO - Done
2021-02-11T13:41:34     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/Lt9aXLUtSU2lhskGjQCJLg/artifacts/public%2Fchain-of-trust.json
2021-02-11T13:41:34     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/Lt9aXLUtSU2lhskGjQCJLg/artifacts/public%2Fchain-of-trust.json.sig
2021-02-11T13:41:34    DEBUG -  makedirs(/app/workdir/cot/XGGmdDBTSPOa1ZFu6Roldg/./public)
2021-02-11T13:41:34     INFO - Done
2021-02-11T13:41:34     INFO - Done
2021-02-11T13:41:34    DEBUG -  makedirs(/app/workdir/cot/Lt9aXLUtSU2lhskGjQCJLg/./public)
2021-02-11T13:41:34     INFO - Done
2021-02-11T13:41:34     INFO - Done
2021-02-11T13:41:34     INFO - Done
2021-02-11T13:41:34    DEBUG -  /app/workdir/cot/BpEU_djMQ8-3PWWPuaxPkQ/./public/chain-of-trust.log downloaded; hash is 810b009ad90244beab2af9a96ef834382a60eb1a44571e1164b343bb33f70c35
2021-02-11T13:41:34    DEBUG -  /app/workdir/cot/An8owHqsSvC9f-9Yok5Z8Q/./public/chain-of-trust.log downloaded; hash is 2fa577d3b0d6d2cddc3dd412cad09424a6f70dfaa98448e9d691534fde0a229d
2021-02-11T13:41:34    DEBUG -  /app/workdir/cot/E7UJGIXgQlKtDOd1YNciZA/./public/chain-of-trust.log downloaded; hash is 050dde841b5a706512c2b8b15ba74490cd7c46a1862802df8d1c23eff190c903
2021-02-11T13:41:34    DEBUG -  /app/workdir/cot/XGGmdDBTSPOa1ZFu6Roldg/./public/chain-of-trust.log downloaded; hash is 61d74cd1c814dedaa99b686edebf85cb4b7ba3372728e5e4164c6cec01a1a98d
2021-02-11T13:41:34    DEBUG -  /app/workdir/cot/Lt9aXLUtSU2lhskGjQCJLg/./public/chain-of-trust.log downloaded; hash is 5ec67192c03fb93a62d2f0bce02eebda21e70c888cafba05b0c33082b4613f5d
2021-02-11T13:41:34    DEBUG -  Verifying the scriptworker:parent BpEU_djMQ8-3PWWPuaxPkQ docker-worker ed25519 chain of trust signature
2021-02-11T13:41:34    DEBUG -  scriptworker:parent BpEU_djMQ8-3PWWPuaxPkQ: ed25519 cot signature verified.
2021-02-11T13:41:34    DEBUG -  Verifying the scriptworker:parent:parent An8owHqsSvC9f-9Yok5Z8Q docker-worker ed25519 chain of trust signature
2021-02-11T13:41:34    DEBUG -  scriptworker:parent:parent An8owHqsSvC9f-9Yok5Z8Q: ed25519 cot signature verified.
2021-02-11T13:41:34    DEBUG -  Verifying the scriptworker:build E7UJGIXgQlKtDOd1YNciZA docker-worker ed25519 chain of trust signature
2021-02-11T13:41:34    DEBUG -  scriptworker:build E7UJGIXgQlKtDOd1YNciZA: ed25519 cot signature verified.
2021-02-11T13:41:34    DEBUG -  Verifying the scriptworker:build:docker-image XGGmdDBTSPOa1ZFu6Roldg docker-worker ed25519 chain of trust signature
2021-02-11T13:41:34    DEBUG -  scriptworker:build:docker-image XGGmdDBTSPOa1ZFu6Roldg: ed25519 cot signature verified.
2021-02-11T13:41:34    DEBUG -  Verifying the scriptworker:build:docker-image:parent Lt9aXLUtSU2lhskGjQCJLg docker-worker ed25519 chain of trust signature
2021-02-11T13:41:34    DEBUG -  scriptworker:build:docker-image:parent Lt9aXLUtSU2lhskGjQCJLg: ed25519 cot signature verified.
2021-02-11T13:41:34    DEBUG -  Verifying public/task-graph.json is in BpEU_djMQ8-3PWWPuaxPkQ cot artifacts...
2021-02-11T13:41:34     INFO - Downloading Chain of Trust artifact:
https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/BpEU_djMQ8-3PWWPuaxPkQ/artifacts/public%2Ftask-graph.json
2021-02-11T13:41:34    DEBUG -  Verifying public/actions.json is in An8owHqsSvC9f-9Yok5Z8Q cot artifacts...
2021-02-11T13:41:34     INFO - Downloading Chain of Trust artifact:
https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/An8owHqsSvC9f-9Yok5Z8Q/artifacts/public%2Factions.json
2021-02-11T13:41:34    DEBUG -  Verifying public/parameters.yml is in An8owHqsSvC9f-9Yok5Z8Q cot artifacts...
2021-02-11T13:41:34     INFO - Downloading Chain of Trust artifact:
https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/An8owHqsSvC9f-9Yok5Z8Q/artifacts/public%2Fparameters.yml
2021-02-11T13:41:34    DEBUG -  Verifying public/task-graph.json is in An8owHqsSvC9f-9Yok5Z8Q cot artifacts...
2021-02-11T13:41:34     INFO - Downloading Chain of Trust artifact:
https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/An8owHqsSvC9f-9Yok5Z8Q/artifacts/public%2Ftask-graph.json
2021-02-11T13:41:34    DEBUG -  Verifying public/actions.json is in Lt9aXLUtSU2lhskGjQCJLg cot artifacts...
2021-02-11T13:41:34     INFO - Downloading Chain of Trust artifact:
https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/Lt9aXLUtSU2lhskGjQCJLg/artifacts/public%2Factions.json
2021-02-11T13:41:34    DEBUG -  Verifying public/parameters.yml is in Lt9aXLUtSU2lhskGjQCJLg cot artifacts...
2021-02-11T13:41:34     INFO - Downloading Chain of Trust artifact:
https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/Lt9aXLUtSU2lhskGjQCJLg/artifacts/public%2Fparameters.yml
2021-02-11T13:41:34    DEBUG -  Verifying public/task-graph.json is in Lt9aXLUtSU2lhskGjQCJLg cot artifacts...
2021-02-11T13:41:34     INFO - Downloading Chain of Trust artifact:
https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/Lt9aXLUtSU2lhskGjQCJLg/artifacts/public%2Ftask-graph.json
2021-02-11T13:41:34    DEBUG -  Verifying public/build/arm64-v8a/target.apk is in E7UJGIXgQlKtDOd1YNciZA cot artifacts...
2021-02-11T13:41:34     INFO - Downloading Chain of Trust artifact:
https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/E7UJGIXgQlKtDOd1YNciZA/artifacts/public%2Fbuild%2Farm64-v8a%2Ftarget.apk
2021-02-11T13:41:34    DEBUG -  Verifying public/build/armeabi-v7a/target.apk is in E7UJGIXgQlKtDOd1YNciZA cot artifacts...
2021-02-11T13:41:34     INFO - Downloading Chain of Trust artifact:
https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/E7UJGIXgQlKtDOd1YNciZA/artifacts/public%2Fbuild%2Farmeabi-v7a%2Ftarget.apk
2021-02-11T13:41:34    DEBUG -  Verifying public/build/x86/target.apk is in E7UJGIXgQlKtDOd1YNciZA cot artifacts...
2021-02-11T13:41:34     INFO - Downloading Chain of Trust artifact:
https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/E7UJGIXgQlKtDOd1YNciZA/artifacts/public%2Fbuild%2Fx86%2Ftarget.apk
2021-02-11T13:41:34    DEBUG -  Verifying public/build/x86_64/target.apk is in E7UJGIXgQlKtDOd1YNciZA cot artifacts...
2021-02-11T13:41:34     INFO - Downloading Chain of Trust artifact:
https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/E7UJGIXgQlKtDOd1YNciZA/artifacts/public%2Fbuild%2Fx86_64%2Ftarget.apk
2021-02-11T13:41:34     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/BpEU_djMQ8-3PWWPuaxPkQ/artifacts/public%2Ftask-graph.json
2021-02-11T13:41:34     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/An8owHqsSvC9f-9Yok5Z8Q/artifacts/public%2Factions.json
2021-02-11T13:41:34     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/An8owHqsSvC9f-9Yok5Z8Q/artifacts/public%2Fparameters.yml
2021-02-11T13:41:34     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/An8owHqsSvC9f-9Yok5Z8Q/artifacts/public%2Ftask-graph.json
2021-02-11T13:41:34     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/Lt9aXLUtSU2lhskGjQCJLg/artifacts/public%2Factions.json
2021-02-11T13:41:34     INFO - Done
2021-02-11T13:41:34     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/Lt9aXLUtSU2lhskGjQCJLg/artifacts/public%2Fparameters.yml
2021-02-11T13:41:34    DEBUG -  /app/workdir/cot/An8owHqsSvC9f-9Yok5Z8Q/public/parameters.yml matches the expected sha256 25658173ddd8ae834f8d5547bd66f0d1152118dac23a358c5f7aa9f54ce3e84d
2021-02-11T13:41:34     INFO - Done
2021-02-11T13:41:34     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/Lt9aXLUtSU2lhskGjQCJLg/artifacts/public%2Ftask-graph.json
2021-02-11T13:41:34    DEBUG -  /app/workdir/cot/An8owHqsSvC9f-9Yok5Z8Q/public/actions.json matches the expected sha256 1e82ceb94b5049b1c9dacf629485ab4fe013d4b1f26b19541b8d318aa504d091
2021-02-11T13:41:34     INFO - Done
2021-02-11T13:41:34     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/E7UJGIXgQlKtDOd1YNciZA/artifacts/public%2Fbuild%2Farm64-v8a%2Ftarget.apk
2021-02-11T13:41:34    DEBUG -  /app/workdir/cot/Lt9aXLUtSU2lhskGjQCJLg/public/parameters.yml matches the expected sha256 4e44911e0823e735d69b06cd5f1ac56e4f8c2d4a57008ae76589db5de58efe58
2021-02-11T13:41:34     INFO - Done
2021-02-11T13:41:34     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/E7UJGIXgQlKtDOd1YNciZA/artifacts/public%2Fbuild%2Farmeabi-v7a%2Ftarget.apk
2021-02-11T13:41:34    DEBUG -  /app/workdir/cot/Lt9aXLUtSU2lhskGjQCJLg/public/task-graph.json matches the expected sha256 53016d3f4f4516fdc8d8ff1ee305bd0d6782674b65a992c739fb0889c221f12c
2021-02-11T13:41:34     INFO - Done
2021-02-11T13:41:34     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/E7UJGIXgQlKtDOd1YNciZA/artifacts/public%2Fbuild%2Fx86%2Ftarget.apk
2021-02-11T13:41:34    DEBUG -  /app/workdir/cot/Lt9aXLUtSU2lhskGjQCJLg/public/actions.json matches the expected sha256 1a09b88fd0157d0dd7fe653de6302f0a09b1edf3b2a09454cce1e37a0d4674da
2021-02-11T13:41:34     INFO - Done
2021-02-11T13:41:34     INFO - Downloading https://firefox-ci-tc.services.mozilla.com/api/queue/v1/task/E7UJGIXgQlKtDOd1YNciZA/artifacts/public%2Fbuild%2Fx86_64%2Ftarget.apk
2021-02-11T13:41:34    DEBUG -  /app/workdir/cot/BpEU_djMQ8-3PWWPuaxPkQ/public/task-graph.json matches the expected sha256 9ace217584c7547285b945abce4f3749082863879b228a76aaaef3bffebc542a
2021-02-11T13:41:34     INFO - Done
2021-02-11T13:41:34    DEBUG -  /app/workdir/cot/An8owHqsSvC9f-9Yok5Z8Q/public/task-graph.json matches the expected sha256 b8e1e88cd15839f3bf7c2970966dc70a451a442db72a50393732fbedfb8ac709
2021-02-11T13:41:34    DEBUG -  makedirs(/app/workdir/cot/E7UJGIXgQlKtDOd1YNciZA/./public/build/x86)
2021-02-11T13:41:34    DEBUG -  makedirs(/app/workdir/cot/E7UJGIXgQlKtDOd1YNciZA/./public/build/armeabi-v7a)
2021-02-11T13:41:34    DEBUG -  makedirs(/app/workdir/cot/E7UJGIXgQlKtDOd1YNciZA/./public/build/arm64-v8a)
2021-02-11T13:41:34    DEBUG -  makedirs(/app/workdir/cot/E7UJGIXgQlKtDOd1YNciZA/./public/build/x86_64)
2021-02-11T13:41:41     INFO - Done
2021-02-11T13:41:42    DEBUG -  /app/workdir/cot/E7UJGIXgQlKtDOd1YNciZA/public/build/armeabi-v7a/target.apk matches the expected sha256 ac6d3d00e37d33deb016bcfdb50f6e1a11f2c66ccc3d889560d1ad090c646eae
2021-02-11T13:41:42     INFO - Done
2021-02-11T13:41:42    DEBUG -  /app/workdir/cot/E7UJGIXgQlKtDOd1YNciZA/public/build/arm64-v8a/target.apk matches the expected sha256 c94d27ecd21e995f91a20900dff2275278e7e388491b204ff7fa57ccde4e5d4a
2021-02-11T13:41:43     INFO - Done
2021-02-11T13:41:43    DEBUG -  /app/workdir/cot/E7UJGIXgQlKtDOd1YNciZA/public/build/x86_64/target.apk matches the expected sha256 891de9ebb881d9c57b8c02fa6be4e43014e11f21982c88c4e85bef1f29ba0f8e
2021-02-11T13:41:43     INFO - Done
2021-02-11T13:41:43    DEBUG -  /app/workdir/cot/E7UJGIXgQlKtDOd1YNciZA/public/build/x86/target.apk matches the expected sha256 0bc8fe2d4b686c1701e000f92bc24ee324542ad0b569f6881a57db550cd726b5
2021-02-11T13:41:43     INFO - Verifying scriptworker EW47NZ4ETr6mCJm9eVWmyQ as a scriptworker task...
2021-02-11T13:41:43     INFO - Verifying scriptworker:parent BpEU_djMQ8-3PWWPuaxPkQ as a action task...
2021-02-11T13:41:43     INFO - Verifying the scriptworker EW47NZ4ETr6mCJm9eVWmyQ task definition is part of the scriptworker:parent BpEU_djMQ8-3PWWPuaxPkQ task graph...
2021-02-11T13:41:43     INFO - Found EW47NZ4ETr6mCJm9eVWmyQ in the graph; it's a match
2021-02-11T13:41:43     INFO - Verifying the scriptworker:build E7UJGIXgQlKtDOd1YNciZA task definition is part of the scriptworker:parent BpEU_djMQ8-3PWWPuaxPkQ task graph...
2021-02-11T13:41:43     INFO - Found E7UJGIXgQlKtDOd1YNciZA in the graph; it's a match
2021-02-11T13:41:43     INFO - Verifying scriptworker:parent BpEU_djMQ8-3PWWPuaxPkQ definition...
2021-02-11T13:41:43    DEBUG -  task_ids: {'default': 'BpEU_djMQ8-3PWWPuaxPkQ', 'decision': 'An8owHqsSvC9f-9Yok5Z8Q'}
2021-02-11T13:41:43    DEBUG -  scriptworker:parent json-e context:
2021-02-11T13:41:43    DEBUG -  {'as_slugid': <function populate_jsone_context.<locals>.<lambda> at 0x7fc247e64e50>,
 'clientId': 'project/releng/shipit/production',
 'input': {'build_number': 1,
           'next_version': '86.0.0-beta.5',
           'previous_graph_ids': ['An8owHqsSvC9f-9Yok5Z8Q'],
           'release_enable_emefree': False,
           'release_enable_partner_attribution': False,
           'release_enable_partner_repack': False,
           'release_eta': None,
           'release_promotion_flavor': 'ship',
           'version': '86.0.0-beta.4'},
 'now': '2021-02-11T13:25:47.235Z',
 'ownTaskId': 'BpEU_djMQ8-3PWWPuaxPkQ',
 'taskGroupId': 'An8owHqsSvC9f-9Yok5Z8Q',
 'taskId': None,
 'tasks_for': 'action'}
2021-02-11T13:41:43    DEBUG -  scriptworker:parent:parent .taskcluster.yml is at https://raw.githubusercontent.com/mozilla-mobile/fenix/a1541a337559b639d8b215bde1691f64f8e0dfcd/.taskcluster.yml
2021-02-11T13:41:43     INFO - Downloading https://raw.githubusercontent.com/mozilla-mobile/fenix/a1541a337559b639d8b215bde1691f64f8e0dfcd/.taskcluster.yml
2021-02-11T13:41:43     INFO - Done
2021-02-11T13:41:43     INFO - scriptworker:parent: Good.
2021-02-11T13:41:43     INFO - Verifying scriptworker:parent:parent An8owHqsSvC9f-9Yok5Z8Q as a decision task...
2021-02-11T13:41:43     INFO - Verifying scriptworker:parent:parent An8owHqsSvC9f-9Yok5Z8Q definition...
2021-02-11T13:41:43    DEBUG -  scriptworker:parent:parent .taskcluster.yml is at https://raw.githubusercontent.com/mozilla-mobile/fenix/a1541a337559b639d8b215bde1691f64f8e0dfcd/.taskcluster.yml
2021-02-11T13:41:44    DEBUG -  task_ids: {'default': 'An8owHqsSvC9f-9Yok5Z8Q', 'decision': 'An8owHqsSvC9f-9Yok5Z8Q'}
2021-02-11T13:41:44    DEBUG -  scriptworker:parent:parent json-e context:
2021-02-11T13:41:44    DEBUG -  {'as_slugid': <function populate_jsone_context.<locals>.<lambda> at 0x7fc247e64c10>,
 'event': {'after': 'a1541a337559b639d8b215bde1691f64f8e0dfcd',
           'before': None,
           'pusher': {'email': 's.kaspari@gmail.com'},
           'ref': 'refs/heads/releases_v86.0.0',
           'repository': {'full_name': 'mozilla-mobile/fenix',
                          'html_url': 'https://github.com/mozilla-mobile/fenix',
                          'name': 'fenix',
                          'pushed_at': None,
                          'ssh_url': 'git@github.com:mozilla-mobile/fenix.git'},
           'sender': {'login': 'pocmo'}},
 'now': '2021-02-10T09:46:19.371Z',
 'ownTaskId': 'An8owHqsSvC9f-9Yok5Z8Q',
 'taskId': None,
 'tasks_for': 'github-push'}
2021-02-11T13:41:44     INFO - scriptworker:parent:parent: Good.
2021-02-11T13:41:44     INFO - Verifying scriptworker:build E7UJGIXgQlKtDOd1YNciZA as a build task...
2021-02-11T13:41:44     INFO - Verifying scriptworker:build:docker-image XGGmdDBTSPOa1ZFu6Roldg as a docker-image task...
2021-02-11T13:41:44     INFO - Verifying scriptworker:build:docker-image:parent Lt9aXLUtSU2lhskGjQCJLg as a decision task...
2021-02-11T13:41:44     INFO - Verifying the scriptworker:build:docker-image XGGmdDBTSPOa1ZFu6Roldg task definition is part of the scriptworker:build:docker-image:parent Lt9aXLUtSU2lhskGjQCJLg task graph...
2021-02-11T13:41:44     INFO - Found XGGmdDBTSPOa1ZFu6Roldg in the graph; it's a match
2021-02-11T13:41:44     INFO - Verifying scriptworker:build:docker-image:parent Lt9aXLUtSU2lhskGjQCJLg definition...
2021-02-11T13:41:44    DEBUG -  scriptworker:build:docker-image:parent .taskcluster.yml is at https://raw.githubusercontent.com/mozilla-mobile/fenix/7d3b23ceeb12c0e3edcb36836e9041b9ca7a1800/.taskcluster.yml
2021-02-11T13:41:44     INFO - Downloading https://raw.githubusercontent.com/mozilla-mobile/fenix/7d3b23ceeb12c0e3edcb36836e9041b9ca7a1800/.taskcluster.yml
2021-02-11T13:41:44     INFO - Done
2021-02-11T13:41:44    DEBUG -  task_ids: {'default': 'Lt9aXLUtSU2lhskGjQCJLg', 'decision': 'Lt9aXLUtSU2lhskGjQCJLg'}
2021-02-11T13:41:45    DEBUG -  scriptworker:build:docker-image:parent json-e context:
2021-02-11T13:41:45    DEBUG -  {'as_slugid': <function populate_jsone_context.<locals>.<lambda> at 0x7fc246da3c10>,
 'event': {'after': '7d3b23ceeb12c0e3edcb36836e9041b9ca7a1800',
           'before': None,
           'pusher': {'email': 'jlorenzo@mozilla.com'},
           'ref': 'refs/heads/master',
           'repository': {'full_name': 'mozilla-mobile/fenix',
                          'html_url': 'https://github.com/mozilla-mobile/fenix',
                          'name': 'fenix',
                          'pushed_at': None,
                          'ssh_url': 'git@github.com:mozilla-mobile/fenix.git'},
           'sender': {'login': 'MihaiTabara'}},
 'now': '2020-11-16T14:22:03.386Z',
 'ownTaskId': 'Lt9aXLUtSU2lhskGjQCJLg',
 'taskId': None,
 'tasks_for': 'github-push'}
2021-02-11T13:41:45     INFO - scriptworker:build:docker-image:parent: Good.
2021-02-11T13:41:45     INFO - Verifying scriptworker EW47NZ4ETr6mCJm9eVWmyQ as a scriptworker task...
2021-02-11T13:41:45     INFO - Verifying scriptworker:parent BpEU_djMQ8-3PWWPuaxPkQ as a docker-worker task...
2021-02-11T13:41:45     INFO - Checking for scriptworker:parent BpEU_djMQ8-3PWWPuaxPkQ interactive docker-worker
2021-02-11T13:41:45     INFO - Verifying scriptworker:parent:parent An8owHqsSvC9f-9Yok5Z8Q as a docker-worker task...
2021-02-11T13:41:45     INFO - Checking for scriptworker:parent:parent An8owHqsSvC9f-9Yok5Z8Q interactive docker-worker
2021-02-11T13:41:45     INFO - Verifying scriptworker:build E7UJGIXgQlKtDOd1YNciZA as a docker-worker task...
2021-02-11T13:41:45     INFO - Checking for scriptworker:build E7UJGIXgQlKtDOd1YNciZA interactive docker-worker
2021-02-11T13:41:45    DEBUG -  Verifying scriptworker:build E7UJGIXgQlKtDOd1YNciZA against docker-image XGGmdDBTSPOa1ZFu6Roldg
2021-02-11T13:41:45    DEBUG -  Found matching docker-image sha ef714fa5c8c2aa0df0d48708f2f5e3a411918a894e21fc5243959c4bb8e00bf2
2021-02-11T13:41:45     INFO - Verifying scriptworker:build:docker-image XGGmdDBTSPOa1ZFu6Roldg as a docker-worker task...
2021-02-11T13:41:45     INFO - Checking for scriptworker:build:docker-image XGGmdDBTSPOa1ZFu6Roldg interactive docker-worker
2021-02-11T13:41:45     INFO - Verifying scriptworker:build:docker-image:parent Lt9aXLUtSU2lhskGjQCJLg as a docker-worker task...
2021-02-11T13:41:45     INFO - Checking for scriptworker:build:docker-image:parent Lt9aXLUtSU2lhskGjQCJLg interactive docker-worker
2021-02-11T13:41:45    DEBUG -  Getting source url for scriptworker EW47NZ4ETr6mCJm9eVWmyQ...
2021-02-11T13:41:45     INFO - scriptworker EW47NZ4ETr6mCJm9eVWmyQ: found https://github.com/mozilla-mobile/fenix/blob/a1541a337559b639d8b215bde1691f64f8e0dfcd/taskcluster/ci/signing
2021-02-11T13:41:45    DEBUG -  Getting source url for scriptworker:parent BpEU_djMQ8-3PWWPuaxPkQ...
2021-02-11T13:41:45     INFO - scriptworker:parent BpEU_djMQ8-3PWWPuaxPkQ: found https://github.com/mozilla-mobile/fenix/raw/a1541a337559b639d8b215bde1691f64f8e0dfcd/.taskcluster.yml
2021-02-11T13:41:45    DEBUG -  Getting source url for scriptworker:parent:parent An8owHqsSvC9f-9Yok5Z8Q...
2021-02-11T13:41:45     INFO - scriptworker:parent:parent An8owHqsSvC9f-9Yok5Z8Q: found https://github.com/mozilla-mobile/fenix/raw/a1541a337559b639d8b215bde1691f64f8e0dfcd/.taskcluster.yml
2021-02-11T13:41:45    DEBUG -  Getting source url for scriptworker:build E7UJGIXgQlKtDOd1YNciZA...
2021-02-11T13:41:45     INFO - scriptworker:build E7UJGIXgQlKtDOd1YNciZA: found https://github.com/mozilla-mobile/fenix/blob/a1541a337559b639d8b215bde1691f64f8e0dfcd/taskcluster/ci/build
2021-02-11T13:41:45    DEBUG -  Getting source url for scriptworker:build:docker-image XGGmdDBTSPOa1ZFu6Roldg...
2021-02-11T13:41:45     INFO - scriptworker:build:docker-image XGGmdDBTSPOa1ZFu6Roldg: found https://github.com/mozilla-mobile/fenix/blob/7d3b23ceeb12c0e3edcb36836e9041b9ca7a1800/taskcluster/ci/docker-image
2021-02-11T13:41:45    DEBUG -  Getting source url for scriptworker:build:docker-image:parent Lt9aXLUtSU2lhskGjQCJLg...
2021-02-11T13:41:45     INFO - scriptworker:build:docker-image:parent Lt9aXLUtSU2lhskGjQCJLg: found https://github.com/mozilla-mobile/fenix/raw/7d3b23ceeb12c0e3edcb36836e9041b9ca7a1800/.taskcluster.yml
2021-02-11T13:41:45     INFO - Found privileged scope project:mobile:fenix:releng:signing:cert:fennec-production-signing
2021-02-11T13:41:45     INFO - Cache does not exist for URL "https://github.com/mozilla-mobile/fenix/branch_commits/a1541a337559b639d8b215bde1691f64f8e0dfcd" (in this context), fetching it...
2021-02-11T13:41:45    DEBUG -  GET https://github.com/mozilla-mobile/fenix/branch_commits/a1541a337559b639d8b215bde1691f64f8e0dfcd
2021-02-11T13:41:45     INFO - Cache does not exist for URL "https://github.com/mozilla-mobile/fenix/branch_commits/7d3b23ceeb12c0e3edcb36836e9041b9ca7a1800" (in this context), fetching it...
2021-02-11T13:41:45    DEBUG -  GET https://github.com/mozilla-mobile/fenix/branch_commits/7d3b23ceeb12c0e3edcb36836e9041b9ca7a1800
2021-02-11T13:41:46    DEBUG -  Status 200
2021-02-11T13:41:46    DEBUG -  Status 200
2021-02-11T13:41:47     INFO - Good.    
"""
        var start = System.nanoTime()
        var regexResult = Regex("""'version': '(.+)'""").find(data)
        println(regexResult?.groups?.get(1))
        var date = Regex("""'now': '(.+)'""").find(data)
        println(date?.groups?.get(1))
        println(System.nanoTime() - start)
    }
}