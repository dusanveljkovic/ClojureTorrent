(ns clojuretorrent.gui.style
  (:require [cljfx.css :as css]))

(def style
  (css/register ::style 
    (let [font-family "'JetBrains Mono', 'Consolas', monospace"
          base-btn-color "#9bccd4"
          red-btn-color "#c96f61"
          yellow-btn-color "#e3dd6b"
          green-btn-color "#9bd180"
          active-btn-color "#6bc7e3"
          btn-border-color "#8c989c"] 
      {".root" 
       {
        :-fx-font-family font-family
        :-fx-padding "2 8 2 8"}

       ".topbar"
       {}

       ".topbar-button"
       {:-fx-font-size "12px"}

       ".seconbar"
       {}

       ".secondbar-btn"
       {:-fx-font-size "14px"
        :-fx-font-weight 700
        :-fx-border-radius 4 
        :-fx-background-radius 4
        :-fx-padding "2 8 2 8"
        :-fx-border-color btn-border-color}

       ".secondbar-btn:hover" 
       {:-fx-background-color "#80a5ab"}

       ".secondbar-btn-base"
       {:-fx-background-color base-btn-color}

       ".secondbar-btn-red"
       {:-fx-background-color red-btn-color}

       ".secondbar-btn-green"
       {:-fx-background-color green-btn-color}

       ".secondbar-btn-yellow"
       {:-fx-background-color yellow-btn-color}

       ".filter-bar"
       {}

       ".filter-btn"
       {:-fx-font-size        "12px"
        :-fx-padding          "4 8 4 8"
        :-fx-border-color btn-border-color
        :-fx-background-radius 4
        :-fx-border-radius 4
        :-fx-cursor           "hand"
        :-fx-background-color base-btn-color}

       ".filter-btn-base"
       {:-fx-background-color base-btn-color}

       ".filter-btn-active"
       {:-fx-background-color active-btn-color
        :-fx-font-weight 600}

       ".table-header"
       {:-fx-background-color "#0d1b33"
        :-fx-padding          "8 16 8 16"}

       ".table-header-label"
       {:-fx-text-fill  "#475569"
        :-fx-font-size  "10px"
        :-fx-font-family font-family}

       ".torrent-row"
       {:-fx-background-color "#1a1a2e"
        :-fx-padding          "10 16 10 16"
        :-fx-border-color     "#1e293b"
        :-fx-border-width     "0 0 1 0"
        :-fx-cursor           "hand"}

       ".torrent-row:hover"
       {:-fx-background-color "#1e2a45"}

       ".torrent-row-selected"
       {:-fx-background-color "#0f2444"
        :-fx-border-color     "#1d4ed8"
        :-fx-border-width     "0 0 1 0 "}

       ".torrent-name"
       {:-fx-text-fill  "#e2e8f0"
        :-fx-font-size  "12px"
        :-fx-font-family font-family}

       ".torrent-meta"
       {:-fx-text-fill  "#64748b"
        :-fx-font-size  "10px"
        :-fx-font-family font-family}

       ".status-downloading"
       {:-fx-text-fill "#38bdf8"
        :-fx-font-size "10px"
        :-fx-font-family font-family}

       ".status-seeding"
       {:-fx-text-fill "#34d399"
        :-fx-font-size "10px"
        :-fx-font-family font-family}

       ".status-paused"
       {:-fx-text-fill "#94a3b8"
        :-fx-font-size "10px"
        :-fx-font-family font-family}

       ".status-error"
       {:-fx-text-fill "#f87171"
        :-fx-font-size "10px"
        :-fx-font-family font-family}

       ".progress-track"
       {:-fx-background-color "#0f2444"
        :-fx-background-radius "1"
        :-fx-pref-height      "3"}

       ".progress-bar-dl"
       {:-fx-background-color "#1d4ed8"
        :-fx-background-radius "1"}

       ".progress-bar-seed"
       {:-fx-background-color "#059669"
        :-fx-background-radius "1"}

       ".progress-bar-paused"
       {:-fx-background-color "#334155"
        :-fx-background-radius "1"}

       ".stat-value"
       {:-fx-text-fill  "#94a3b8"
        :-fx-font-size  "11px"
        :-fx-font-family font-family}

       ".statusbar"
       {:-fx-background-color "#0d1117"
        :-fx-padding          "5 16 5 16"
        :-fx-border-color     "#0f3460"
        :-fx-border-width     "1 0 0 0"}

       ".statusbar-label"
       {:-fx-text-fill  "#475569"
        :-fx-font-size  "10px"
        :-fx-font-family font-family}})))
