rm -rf ansible monit microservice_spec.rb spec_helper.rb Vagrantfile

mkdir rpm-build
mv jenkins_job rpm-build
mv jenkins_job_make_rpm rpm-build
mv pkg-scripts rpm-build/.
mv upstart rpm-build/.
git add -A .
git add -f rpm-build/upstart/os-address-lookup.conf
git rm --cached rpm_build.sh
git status
cd rpm-build
