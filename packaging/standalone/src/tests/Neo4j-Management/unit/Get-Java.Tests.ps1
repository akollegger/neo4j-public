# Copyright (c) "Neo4j"
# Neo4j Sweden AB [http://neo4j.com]
#
# This file is part of Neo4j.
#
# Neo4j is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$sut = (Split-Path -Leaf $MyInvocation.MyCommand.Path).Replace(".Tests.",".")
$common = Join-Path (Split-Path -Parent $here) 'Common.ps1'
.$common

Import-Module "$src\Neo4j-Management.psm1"

InModuleScope Neo4j-Management {
  Describe "Get-Java" {

    # Setup mocking environment
    #  Mock Java environment
    $javaHome = global:New-MockJavaHome
    Mock Get-Neo4jEnv { $javaHome } -ParameterFilter { $Name -eq 'JAVA_HOME' }
    Mock Test-Path { $false } -ParameterFilter {
      $Path -like 'Registry::*\JavaSoft\JRE'
    }
    Mock Get-ItemProperty { $null } -ParameterFilter {
      $Path -like 'Registry::*\JavaSoft\JRE*'
    }
    Mock Get-JavaVersion { @{ 'isValid' = $true; 'isJava11' = $true } }

    # Java Detection Tests
    Context "Valid Java install in JAVA_HOME environment variable" {
      $result = Get-Java

      It "should return java location" {
        $result.java | Should Be "$javaHome\bin\java.exe"
      }

      It "should have empty shell arguments" {
        $result.args | Should BeNullOrEmpty
      }
    }

    Context "Legacy Java install in JAVA_HOME environment variable" {
      Mock Get-JavaVersion -Verifiable { @{ 'isValid' = $false; 'isJava11' = $false } }

      It "should throw if java is not supported" {
        { Get-Java -ErrorAction Stop } | Should Throw
      }

      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }

    Context "Invalid Java install in JAVA_HOME environment variable" {
      Mock Test-Path { $false } -ParameterFile { $Path -like "$javaHome\bin\java.exe" }

      It "should throw if java missing" {
        { Get-Java -ErrorAction Stop } | Should Throw
      }
    }

    Context "Valid JRE install in Registry (32bit JRE on 64bit OS)" {
      Mock Get-Neo4jEnv { $null } -ParameterFilter { $Name -eq 'JAVA_HOME' }
      Mock Test-Path -Verifiable { return $false } -ParameterFilter {
        ($Path -in @('Registry::HKLM\SOFTWARE\JavaSoft\JDK', 'Registry::HKLM\SOFTWARE\Wow6432Node\JavaSoft\JDK', 'Registry::HKLM\SOFTWARE\JavaSoft\JRE'))
      }
      Mock Test-Path -Verifiable { return $true } -ParameterFilter {
        ($Path -eq 'Registry::HKLM\SOFTWARE\Wow6432Node\JavaSoft\JRE')
      }
      Mock Get-ItemProperty -Verifiable { return @{ 'CurrentVersion' = '9.9' } } -ParameterFilter {
        ($Path -eq 'Registry::HKLM\SOFTWARE\Wow6432Node\JavaSoft\JRE')
      }
      Mock Get-ItemProperty -Verifiable { return @{ 'JavaHome' = $javaHome } } -ParameterFilter {
        ($Path -eq 'Registry::HKLM\SOFTWARE\Wow6432Node\JavaSoft\JRE\9.9')
      }

      $result = Get-Java

      It "should return java location from registry" {
        $result.java | Should Be "$javaHome\bin\java.exe"
      }

      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }

    Context "Valid JRE install in Registry" {
      Mock Get-Neo4jEnv { $null } -ParameterFilter { $Name -eq 'JAVA_HOME' }
      Mock Test-Path -Verifiable { return $false } -ParameterFilter {
        ($Path -in @('Registry::HKLM\SOFTWARE\JavaSoft\JDK', 'Registry::HKLM\SOFTWARE\Wow6432Node\JavaSoft\JDK'))
      }
      Mock Test-Path -Verifiable { return $true } -ParameterFilter {
        ($Path -eq 'Registry::HKLM\SOFTWARE\JavaSoft\JRE')
      }
      Mock Get-ItemProperty -Verifiable { return @{ 'CurrentVersion' = '9.9' } } -ParameterFilter {
        ($Path -eq 'Registry::HKLM\SOFTWARE\JavaSoft\JRE')
      }
      Mock Get-ItemProperty -Verifiable { return @{ 'JavaHome' = $javaHome } } -ParameterFilter {
        ($Path -eq 'Registry::HKLM\SOFTWARE\JavaSoft\JRE\9.9')
      }

      $result = Get-Java

      It "should return java location from registry" {
        $result.java | Should Be "$javaHome\bin\java.exe"
      }

      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }

    Context "Invalid JRE install in Registry" {
      Mock Test-Path { $false } -ParameterFile { $Path -like "$javaHome\bin\java.exe" }
      Mock Get-Neo4jEnv { $null } -ParameterFilter { $Name -eq 'JAVA_HOME' }
      Mock Test-Path -Verifiable { return $false } -ParameterFilter {
        ($Path -in @('Registry::HKLM\SOFTWARE\JavaSoft\JDK', 'Registry::HKLM\SOFTWARE\Wow6432Node\JavaSoft\JDK'))
      }
      Mock Test-Path -Verifiable { return $true } -ParameterFilter {
        ($Path -eq 'Registry::HKLM\SOFTWARE\JavaSoft\JRE')
      }
      Mock Get-ItemProperty -Verifiable { return @{ 'CurrentVersion' = '9.9' } } -ParameterFilter {
        ($Path -eq 'Registry::HKLM\SOFTWARE\JavaSoft\JRE')
      }
      Mock Get-ItemProperty -Verifiable { return @{ 'JavaHome' = $javaHome } } -ParameterFilter {
        ($Path -eq 'Registry::HKLM\SOFTWARE\JavaSoft\JRE\9.9')
      }

      It "should throw if java missing" {
        { Get-Java -ErrorAction Stop } | Should Throw
      }

      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }

    Context "Valid JDK install in Registry (32bit JDK on 64bit OS)" {
      Mock Get-Neo4jEnv { $null } -ParameterFilter { $Name -eq 'JAVA_HOME' }
      Mock Test-Path -Verifiable { return $false } -ParameterFilter {
        ($Path -in @('Registry::HKLM\SOFTWARE\JavaSoft\JDK'))
      }
      Mock Test-Path -Verifiable { return $true } -ParameterFilter {
        ($Path -eq 'Registry::HKLM\SOFTWARE\Wow6432Node\JavaSoft\JDK')
      }
      Mock Get-ItemProperty -Verifiable { return @{ 'CurrentVersion' = '9.9' } } -ParameterFilter {
        ($Path -eq 'Registry::HKLM\SOFTWARE\Wow6432Node\JavaSoft\JDK')
      }
      Mock Get-ItemProperty -Verifiable { return @{ 'JavaHome' = $javaHome } } -ParameterFilter {
        ($Path -eq 'Registry::HKLM\SOFTWARE\Wow6432Node\JavaSoft\JDK\9.9')
      }

      $result = Get-Java

      It "should return java location from registry" {
        $result.java | Should Be "$javaHome\bin\java.exe"
      }

      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }

    Context "Valid JDK install in Registry" {
      Mock Get-Neo4jEnv { $null } -ParameterFilter { $Name -eq 'JAVA_HOME' }
      Mock Test-Path -Verifiable { return $true } -ParameterFilter {
        ($Path -eq 'Registry::HKLM\SOFTWARE\JavaSoft\JDK')
      }
      Mock Get-ItemProperty -Verifiable { return @{ 'CurrentVersion' = '9.9' } } -ParameterFilter {
        ($Path -eq 'Registry::HKLM\SOFTWARE\JavaSoft\JDK')
      }
      Mock Get-ItemProperty -Verifiable { return @{ 'JavaHome' = $javaHome } } -ParameterFilter {
        ($Path -eq 'Registry::HKLM\SOFTWARE\JavaSoft\JDK\9.9')
      }

      $result = Get-Java

      It "should return java location from registry" {
        $result.java | Should Be "$javaHome\bin\java.exe"
      }

      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }

    Context "Invalid JDK install in Registry" {
      Mock Test-Path { $false } -ParameterFile { $Path -like "$javaHome\bin\java.exe" }
      Mock Get-Neo4jEnv { $null } -ParameterFilter { $Name -eq 'JAVA_HOME' }
      Mock Test-Path -Verifiable { return $true } -ParameterFilter {
        ($Path -eq 'Registry::HKLM\SOFTWARE\JavaSoft\JDK')
      }
      Mock Get-ItemProperty -Verifiable { return @{ 'CurrentVersion' = '9.9' } } -ParameterFilter {
        ($Path -eq 'Registry::HKLM\SOFTWARE\JavaSoft\JDK')
      }
      Mock Get-ItemProperty -Verifiable { return @{ 'JavaHome' = $javaHome } } -ParameterFilter {
        ($Path -eq 'Registry::HKLM\SOFTWARE\JavaSoft\JDK\9.9')
      }

      It "should throw if java missing" {
        { Get-Java -ErrorAction Stop } | Should Throw
      }

      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }

    Context "Valid Java install in search path" {
      Mock Get-Neo4jEnv { $null } -ParameterFilter { $Name -eq 'JAVA_HOME' }
      Mock Test-Path -Verifiable { return $false } -ParameterFilter {
        ($Path -in @('Registry::HKLM\SOFTWARE\JavaSoft\JDK', 'Registry::HKLM\SOFTWARE\Wow6432Node\JavaSoft\JDK', 'Registry::HKLM\SOFTWARE\JavaSoft\JRE', 'Registry::HKLM\SOFTWARE\Wow6432Node\JavaSoft\JRE'))
      }
      Mock Get-Command -Verifiable { return @{ 'Path' = "$javaHome\bin\java.exe" } }

      $result = Get-Java

      It "should return java location from search path" {
        $result.java | Should Be "$javaHome\bin\java.exe"
      }

      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }

    Context "No Java install at all" {
      Mock Get-Neo4jEnv { $null } -ParameterFilter { $Name -eq 'JAVA_HOME' }
      Mock Test-Path -Verifiable { return $false } -ParameterFilter {
        ($Path -in @('Registry::HKLM\SOFTWARE\JavaSoft\JDK', 'Registry::HKLM\SOFTWARE\Wow6432Node\JavaSoft\JDK', 'Registry::HKLM\SOFTWARE\JavaSoft\JRE', 'Registry::HKLM\SOFTWARE\Wow6432Node\JavaSoft\JRE'))
      }
      Mock Get-Command { $null }

      It "should throw if java not detected" {
        { Get-Java -ErrorAction Stop } | Should Throw
      }
    }

    # ForServer tests
    Context "Server Invoke - Community v4.0" {
      $serverObject = global:New-MockNeo4jInstall -ServerVersion '4.0' -ServerType 'Community'

      $result = Get-Java -ForServer -Neo4jServer $serverObject
      $resultArgs = ($result.args -join ' ')

      It "should have main class of org.neo4j.server.CommunityEntryPoint" {
        $resultArgs | Should Match ([regex]::Escape(' org.neo4j.server.CommunityEntryPoint'))
      }
    }

    Context "Server Invoke - Should set heap size" {
      $serverObject = global:New-MockNeo4jInstall -ServerVersion '4.0' -ServerType 'Community' `
         -NeoConfSettings 'dbms.memory.heap.initial_size=123k','dbms.memory.heap.max_size=234g'

      $result = Get-Java -ForServer -Neo4jServer $serverObject
      $resultArgs = ($result.args -join ' ')

      It "should set initial heap size" {
        $resultArgs | Should Match ([regex]::Escape(' -Xms123k '))
      }

      It "should set max heap size" {
        $resultArgs | Should Match ([regex]::Escape(' -Xmx234g '))
      }
    }

    Context "Server Invoke - Should default heap size unit to megabytes" {
      $serverObject = global:New-MockNeo4jInstall -ServerVersion '4.0' -ServerType 'Community' `
         -NeoConfSettings 'dbms.memory.heap.initial_size=123','dbms.memory.heap.max_size=234'

      $result = Get-Java -ForServer -Neo4jServer $serverObject
      $resultArgs = ($result.args -join ' ')

      It "should set initial heap size" {
        $resultArgs | Should Match ([regex]::Escape(' -Xms123m '))
      }

      It "should set max heap size" {
        $resultArgs | Should Match ([regex]::Escape(' -Xmx234m '))
      }
    }

    Context "Server Invoke - Enable Default GC Logs" {
      Mock Get-JavaVersion { @{ 'isValid' = $true; 'isJava11' = $false } }
      
      $serverObject = global:New-MockNeo4jInstall -ServerVersion '4.0' -ServerType 'Community' `
         -NeoConfSettings 'dbms.logs.gc.enabled=true'

      $result = Get-Java -ForServer -Neo4jServer $serverObject
      $resultArgs = ($result.args -join ' ')

      It "should set default options" {
        $resultArgs | Should Match ([regex]::Escape('-Xlog:gc*,safepoint,age*=trace'))
      }

      It "should set gc log file size" {
        $resultArgs | Should Match ([regex]::Escape('filesize='))
      }

      It "should set number of gc logs" {
        $resultArgs | Should Match ([regex]::Escape('filecount='))
      }

      It "should set file location" {
        $resultArgs | Should Match ([regex]::Escape('file='))
      }
    }

    Context "Server Invoke - Enable Specific GC Logs" {
      Mock Get-JavaVersion { @{ 'isValid' = $true; 'isJava11' = $false } }

      $serverObject = global:New-MockNeo4jInstall -ServerVersion '4.0' -ServerType 'Community' `
         -NeoConfSettings 'dbms.logs.gc.enabled=true','dbms.logs.gc.options=key1=value1 key2=value2'

      $result = Get-Java -ForServer -Neo4jServer $serverObject
      $resultArgs = ($result.args -join ' ')

      It "should set gc file" {
        $resultArgs | Should Match ([regex]::Escape('file='))
      }

      It "should set gc log file size" {
        $resultArgs | Should Match ([regex]::Escape('filesize='))
      }

      It "should set number of gc log files" {
        $resultArgs | Should Match ([regex]::Escape('filecount='))
      }

      It "should set specific options" {
        $resultArgs | Should Match ([regex]::Escape(' key1=value1'))
        $resultArgs | Should Match ([regex]::Escape(' key2=value2'))
      }
    }

    # Utility Invoke
    Context "Utility Invoke" {
      $serverObject = global:New-MockNeo4jInstall -ServerVersion '99.99' -ServerType 'Community'

      $result = Get-Java -ForUtility -StartingClass 'someclass' -Neo4jServer $serverObject -ErrorAction Stop
      $resultArgs = ($result.args -join ' ')

      It "should have jars from bin" {
        $resultArgs | Should Match ([regex]::Escape("$($serverObject.Home)/bin/*"))
      }
      It "should have jars from lib" {
        $resultArgs | Should Match ([regex]::Escape("$($serverObject.Home)/lib/*"))
      }
      It "should have correct Starting Class" {
        $resultArgs | Should Match ([regex]::Escape(' someclass'))
      }
    }

    Context "Utility Invoke - Should set heap size from environment variable" {
      $serverObject = global:New-MockNeo4jInstall -ServerVersion '99.99' -ServerType 'Community'
      Mock Get-Neo4jEnv { '666m' } -ParameterFilter { $Name -eq 'HEAP_SIZE' }

      $result = Get-Java -ForUtility -StartingClass 'someclass' -Neo4jServer $serverObject -ErrorAction Stop
      $resultArgs = ($result.args -join ' ')

      It "should have jars from bin" {
        $resultArgs | Should Match ([regex]::Escape("$($serverObject.Home)/bin/*"))
      }
      It "should have jars from lib" {
        $resultArgs | Should Match ([regex]::Escape("$($serverObject.Home)/lib/*"))
      }
      It "should have correct Starting Class" {
        $resultArgs | Should Match ([regex]::Escape(' someclass'))
      }
      It "should have correct initial heap" {
        $resultArgs | Should Match ([regex]::Escape('-Xms666m'))
      }
      It "should have correct maximum heap" {
        $resultArgs | Should Match ([regex]::Escape('-Xmx666m'))
      }
    }

    Context "Utility Invoke - Should provide parallel collector option" {
      $serverObject = global:New-MockNeo4jInstall -ServerVersion '99.99' -ServerType 'Community'

      $result = Get-Java -ForUtility -StartingClass 'someclass' -Neo4jServer $serverObject -ErrorAction Stop
      $resultArgs = ($result.args -join ' ')

      It "should have parallel collector java option" {
        $resultArgs | Should Match ([regex]::Escape('-XX:+UseParallelGC'))
      }
    }

    Context "Server Invoke - Should handle paths with spaces" {
      $serverObject = global:New-MockNeo4jInstall -ServerVersion '4.0' -ServerType 'Community' `
         -RootDir 'TestDrive:\Neo4j Home' `
         -NeoConfSettings 'dbms.logs.gc.enabled=true'

      $result = Get-Java -ForServer -Neo4jServer $serverObject
      $argList = $result.args

      It "should have literal quotes around config path" {
        $argList -contains "--config-dir=`"TestDrive:\Neo4j Home\conf`"" | Should Be True
      }
      It "should have literal quotes around home path" {
        $argList -contains "--home-dir=`"TestDrive:\Neo4j Home`"" | Should Be True
      }
      It "should have literal quotes around gclog path" {
        $argList -contains
        "-Xlog:gc*,safepoint,age*=trace:file=\""TestDrive:\Neo4j Home\logs/gc.log\""::filecount=5,filesize=20m"| Should Be True
      }
    }

    Context "Additional Arguments test" {
      It "Should add additional argument to java arguments" {
        $serverObject = global:New-MockNeo4jInstall -AdditionalArguments @("foo")
        $result = Get-Java -ForServer -Neo4jServer $serverObject
        $result.args -contains "foo" | Should Be True
      }
    }
  }
}
