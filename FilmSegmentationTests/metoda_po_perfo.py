import cv2
import numpy as np

def find_rectangular_contours(img, min_area=50, max_area=5000, aspect_ratio_range=(0.3, 3.0)):
    """
    Znajduje prostokątne kontury (np. perforacje na taśmie)
    
    Args:
        img: obraz (skala szarości lub BGR)
        min_area: minimalna powierzchnia konturu
        max_area: maksymalna powierzchnia konturu
        aspect_ratio_range: zakres proporcji (w, h)
    
    Returns:
        rectangles: lista prostokątów [(x, y, w, h), ...]
        contours: lista konturów
    """
    
    # Konwersja do skali szarości jeśli trzeba
    if len(img.shape) == 3:
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    else:
        gray = img.copy()
    
    # Preprocessing
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    
    # Progowanie adaptacyjne - lepsze dla nierównomiernego oświetlenia
    binary = cv2.adaptiveThreshold(
        blurred, 255, 
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C, 
        cv2.THRESH_BINARY_INV, 
        blockSize=11, 
        C=2
    )
    
    # Lub zwykłe progowanie Otsu
    # _, binary = cv2.threshold(blurred, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)
    
    # Operacje morfologiczne - zamknięcie małych dziur
    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (3, 3))
    binary = cv2.morphologyEx(binary, cv2.MORPH_CLOSE, kernel, iterations=1)
    
    # Znajdź kontury
    contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    
    rectangles = []
    valid_contours = []
    
    for contour in contours:
        area = cv2.contourArea(contour)
        
        # Filtruj po powierzchni
        if area < min_area or area > max_area:
            continue
        
        # Aproksymuj kontur wielokątem
        epsilon = 0.02 * cv2.arcLength(contour, True)
        approx = cv2.approxPolyDP(contour, epsilon, True)
        
        # Sprawdź czy to prostokąt (4 wierzchołki)
        if len(approx) == 4:
            x, y, w, h = cv2.boundingRect(approx)
            aspect_ratio = float(w) / h if h > 0 else 0
            
            # Sprawdź proporcje
            if aspect_ratio_range[0] <= aspect_ratio <= aspect_ratio_range[1]:
                rectangles.append((x, y, w, h))
                valid_contours.append(approx)
        
        # Alternatywnie: sprawdź "prostokątność" bez aproksymacji
        else:
            x, y, w, h = cv2.boundingRect(contour)
            rect_area = w * h
            extent = area / rect_area if rect_area > 0 else 0
            
            # Jeśli kontur wypełnia bounding box w ~80-100%, to prawdopodobnie prostokąt
            if extent > 0.8:
                aspect_ratio = float(w) / h if h > 0 else 0
                if aspect_ratio_range[0] <= aspect_ratio <= aspect_ratio_range[1]:
                    rectangles.append((x, y, w, h))
                    valid_contours.append(contour)
    
    return rectangles, valid_contours, binary


def detect_sprocket_holes(img, show_steps=False):
    """
    Wykrywa perforacje (sprocket holes) na taśmie filmowej
    """
    
    original = img.copy()
    if len(original.shape) == 2:
        original = cv2.cvtColor(original, cv2.COLOR_GRAY2BGR)
    
    # Szukaj małych prostokątów - perforacje to ~4-8mm na taśmie 35mm
    # W zależności od rozdzielczości skanu, dostosuj min/max area
    rectangles, contours, binary = find_rectangular_contours(
        img,
        min_area=100,      # Dostosuj do rozdzielczości
        max_area=2000,     # Dostosuj do rozdzielczości
        aspect_ratio_range=(0.5, 2.5)  # Perforacje mogą być kwadratowe lub lekko wydłużone
    )
    
    print(f"Znaleziono {len(rectangles)} prostokątnych konturów")
    
    # Filtruj po położeniu - perforacje są na krawędziach
    img_height, img_width = img.shape[:2]
    edge_margin = img_width * 0.15  # Perforacje w 15% z każdej strony
    
    left_holes = []
    right_holes = []
    
    for (x, y, w, h) in rectangles:
        center_x = x + w/2
        
        # Lewa krawędź
        if center_x < edge_margin:
            left_holes.append((x, y, w, h))
        # Prawa krawędź
        elif center_x > img_width - edge_margin:
            right_holes.append((x, y, w, h))
    
    print(f"Perforacje: {len(left_holes)} po lewej, {len(right_holes)} po prawej")
    
    # Rysuj wyniki
    result = original.copy()
    
    # Rysuj wszystkie prostokąty na szaro
    for (x, y, w, h) in rectangles:
        cv2.rectangle(result, (x, y), (x+w, y+h), (128, 128, 128), 2)
    
    # Rysuj perforacje po lewej na niebiesko
    for (x, y, w, h) in left_holes:
        cv2.rectangle(result, (x, y), (x+w, y+h), (255, 0, 0), 2)
    
    # Rysuj perforacje po prawej na czerwono
    for (x, y, w, h) in right_holes:
        cv2.rectangle(result, (x, y), (x+w, y+h), (0, 0, 255), 2)
    
    # Jeśli mamy perforacje, znajdź granice klatki
    frame_bounds = None
    if len(left_holes) >= 2 or len(right_holes) >= 2:
        frame_bounds = estimate_frame_from_sprockets(left_holes, right_holes, img.shape)
        
        if frame_bounds is not None:
            x1, y1, x2, y2 = frame_bounds
            cv2.rectangle(result, (x1, y1), (x2, y2), (0, 255, 0), 3)
            print(f"Granice klatki: x={x1}-{x2}, y={y1}-{y2}")
    
    if show_steps:
        cv2.imshow('Binary', cv2.resize(binary, (800, 600)))
        cv2.imshow('Detected Rectangles', cv2.resize(result, (800, 600)))
        cv2.waitKey(0)
        cv2.destroyAllWindows()
    
    return result, left_holes, right_holes, frame_bounds


def estimate_frame_from_sprockets(left_holes, right_holes, img_shape):
    """
    Estymuje granice klatki filmowej na podstawie perforacji
    """
    if len(left_holes) < 2 and len(right_holes) < 2:
        return None
    
    img_height, img_width = img_shape[:2]
    
    # Znajdź skrajne perforacje po Y (góra i dół klatki)
    all_holes = left_holes + right_holes
    
    if not all_holes:
        return None
    
    # Sortuj po Y
    sorted_by_y = sorted(all_holes, key=lambda h: h[1])
    
    # Granice Y: od górnej do dolnej perforacji
    top_hole = sorted_by_y[0]
    bottom_hole = sorted_by_y[-1]
    
    y1 = top_hole[1]
    y2 = bottom_hole[1] + bottom_hole[3]
    
    # Granice X: między perforacjami
    # Lewa krawędź klatki = za perforacjami lewymi
    x1 = 0
    if left_holes:
        rightmost_left = max(left_holes, key=lambda h: h[0] + h[2])
        x1 = rightmost_left[0] + rightmost_left[2]
    
    # Prawa krawędź klatki = przed perforacjami prawymi
    x2 = img_width
    if right_holes:
        leftmost_right = min(right_holes, key=lambda h: h[0])
        x2 = leftmost_right[0]
    
    return (x1, y1, x2, y2)


# Przykład użycia
if __name__ == "__main__":
    img = cv2.imread("a.jpg")
    
    # Dla negatywu
    img_gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    img_gray = ~img_gray
    
    result, left, right, bounds = detect_sprocket_holes(img_gray, show_steps=True)
    
    cv2.imwrite("sprocket_detection.jpg", result)