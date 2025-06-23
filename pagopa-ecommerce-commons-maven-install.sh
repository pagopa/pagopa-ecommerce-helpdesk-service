#bin/sh
version=$1
checkoutFolder=checkouts
gitRepo=https://github.com/pagopa/pagopa-ecommerce-commons
rm -rf $checkoutFolder
mkdir $checkoutFolder

cd $checkoutFolder

echo "Cloning ecommerce commons repo... $gitRepo"
git clone $gitRepo
cd pagopa-ecommerce-commons
echo "Checking out ecommerce common ref $version"
git checkout $version
# compile with java 17 compatibility even when running on java 21
echo "Building with Java 17 compatibility..."
./mvnw install -DskipTests -Dmaven.compiler.source=17 -Dmaven.compiler.target=17 -Dmaven.compiler.release=17

cd ../../
rm -rf $checkoutFolder