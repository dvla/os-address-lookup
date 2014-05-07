require_relative './spec_helper.rb'

describe file('/opt/qas-address-lookup/vehiclesServices.conf') do
  its(:content) { should include 'vss.baseurl'  }
 end

upstart_services = [ 'qas-address-lookup' ]

upstart_services.each do |item|
  describe command("initctl status #{item}") do
    its(:stdout) {should include "start/running"}
  end
end


sysV_services = [ 'monit' ]

sysV_services.each do |item|
  describe service(item) do
    it { should be_running }
  end
end


ports = [ 8083 ]

ports.each do |item|
  describe command('netstat -anl | grep LISTEN | grep -v ING') do
    its(:stdout) {should include "#{item}" }
  end
end



