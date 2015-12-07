var app = angular.module('euro', []);
app.controller('currenciesCtrl', function($scope, $http) {
    $http.get("http://localhost:8080/currencies")
    .then(function(response) {$scope.currencies = response.data.currencies;});
    $http.get("http://localhost:8080/dates")
    .then(function(response) {$scope.dates = response.data.dates;});
    $scope.euroVal = 0;
    $scope.change = function() {
        $http.get("http://localhost:8080/rate?currency="+$scope.currencyName+"&date="+$scope.currencyDate)
    	.then(function(response) {$scope.response = response.data;});
    	$scope.euroVal=parseFloat($scope.amountOther/$scope.response.rate).toFixed(2);
      };
    $scope.changeEuros = function() {
        $http.get("http://localhost:8080/rate?currency="+$scope.toCurrencyName+"&date="+$scope.atCurrencyDate)
    	.then(function(response) {$scope.response = response.data;});
    	$scope.otherVal=parseFloat($scope.amountEuros*$scope.response.rate).toFixed(2);
      };
});