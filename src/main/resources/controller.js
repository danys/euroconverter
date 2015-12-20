var app = angular.module('euro', []);
app.controller('currenciesCtrl', function($scope, $http) {
    $http.get("http://localhost:8080/currencies")
    .then(function(response) {$scope.currencies = response.data.records;});
});