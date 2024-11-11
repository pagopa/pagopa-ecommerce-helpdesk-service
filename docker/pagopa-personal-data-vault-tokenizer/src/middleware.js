module.exports = (req, res, next) => {
  const db = req.app.db
  if (req.body.pii === 'notfound@example.com') {
    res
    .status(200)
    .jsonp(db.get("tokens-not-found"));
  }
  req.method = 'GET'
  next()
}